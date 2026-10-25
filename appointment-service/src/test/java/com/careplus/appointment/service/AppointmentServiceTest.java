package com.careplus.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.careplus.appointment.model.AppointmentDto;
import com.careplus.appointment.repository.AppointmentRepository;
import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.DoctorAvialability;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.Status;

import jakarta.persistence.EntityManager;

/**
 * Booking, viewing and cancelling as a patient. Plain fakes instead of Mockito: a fake repository, a fake
 * EntityManager and a fake RestTemplate that plays patient-service and doctor-service.
 */
class AppointmentServiceTest {

	private static final String AUTH = "Bearer test-token";
	private static final LocalDate DAY = LocalDate.of(2030, 1, 15);

	private final Map<Long, Appointment> store = new HashMap<>();
	private FakeRest rest;
	private AppointmentService service;
	private long nextId = 100;

	@BeforeEach
	void setUp() throws Exception {
		rest = new FakeRest();
		AppointmentRepository repo = (AppointmentRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class[] { AppointmentRepository.class }, (proxy, m, args) -> switch (m.getName()) {
					case "findById" -> Optional.ofNullable(store.get(args[0]));
					case "saveAndFlush" -> {
						Appointment a = (Appointment) args[0];
						if (a.getId() == null) {
							a.setId(nextId++);
						}
						store.put(a.getId(), a);
						yield a;
					}
					case "findByDoctor_IdAndAppointmentDateAndAppointmentTime" -> store.values().stream().filter(a ->
							a.getDoctor().getId().equals(args[0]) && a.getAppointmentDate().equals(args[1])
									&& a.getAppointmentTime().equals(args[2])).findFirst();
					default -> throw new UnsupportedOperationException(m.getName());
				});
		EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class[] { EntityManager.class }, (proxy, m, args) -> {
					Object o = ((Class<?>) args[0]).getDeclaredConstructor().newInstance();
					o.getClass().getMethod("setId", Long.class).invoke(o, args[1]);
					return o;
				});
		service = new AppointmentService();
		set("repo", repo);
		set("template", rest);
		set("entityManager", em);
		set("patientServiceUrl", "http://patients");
		set("doctorServiceUrl", "http://doctors");
		set("jwtService", new JwtService() {
			@Override
			public String generateServiceToken() {
				return "service-token";
			}
		});
		rest.slots.add(slot(7L, LocalTime.of(9, 0), LocalTime.of(12, 0)));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	// ---- booking ----

	@Test
	void patientBookingUsesTheLoggedInPatientAndStatusBooked() throws Exception {
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		AppointmentDto dto = booking(null, 3L);
		dto.setStatus("CANCELLED"); // a client must not choose the status

		AppointmentDto saved = service.createAppointment(dto, AUTH);

		assertThat(saved.getId()).isNotNull();
		Appointment stored = store.get(saved.getId());
		assertThat(stored.getPatient().getId()).isEqualTo(5L);
		assertThat(stored.getStatus()).isEqualTo(Status.BOOKED);
		assertThat(rest.puts).containsExactly("http://doctors/api/doctors/3/availability/7/decrement");
	}

	@Test
	void anotherPatientCanBookASlotThatWasCancelledAndTheCancelledRowIsReused() throws Exception {
		store.put(1L, appointment(1L, 5L, 3L, Status.CANCELLED)); // doctor 3, DAY 10:30
		loginAs("grace@example.com", "ROLE_PATIENT");
		rest.myPatientId = 6L;

		AppointmentDto saved = service.createAppointment(booking(null, 3L), AUTH);

		// Same row, so the unique key (doctor, date, time) is never violated.
		assertThat(saved.getId()).isEqualTo(1L);
		assertThat(store).hasSize(1);
		Appointment row = store.get(1L);
		assertThat(row.getStatus()).isEqualTo(Status.BOOKED);
		assertThat(row.getPatient().getId()).isEqualTo(6L);
		assertThat(row.getReason()).isEqualTo("check-up");
		assertThat(rest.puts).containsExactly("http://doctors/api/doctors/3/availability/7/decrement");
	}

	@Test
	void cancelThenRebookTheSameSlotWorksForTheSamePatient() throws Exception {
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		AppointmentDto first = service.createAppointment(booking(null, 3L), AUTH);
		service.cancelAppointment(first.getId(), AUTH);
		AppointmentDto again = service.createAppointment(booking(null, 3L), AUTH);

		assertThat(again.getId()).isEqualTo(first.getId());
		assertThat(store.get(again.getId()).getStatus()).isEqualTo(Status.BOOKED);
		assertThat(rest.puts).containsExactly(
				"http://doctors/api/doctors/3/availability/7/decrement",
				"http://doctors/api/doctors/3/availability/7/increment",
				"http://doctors/api/doctors/3/availability/7/decrement");
	}

	@Test
	void aBookedOrCompletedSlotIsStillTaken() {
		store.put(1L, appointment(1L, 6L, 3L, Status.BOOKED));
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		assertThatThrownBy(() -> service.createAppointment(booking(null, 3L), AUTH))
				.isInstanceOf(com.careplus.appointment.exception.AppointmentExistsException.class);

		store.get(1L).setStatus(Status.COMPLETED);
		assertThatThrownBy(() -> service.createAppointment(booking(null, 3L), AUTH))
				.isInstanceOf(com.careplus.appointment.exception.AppointmentExistsException.class);
		assertThat(store.get(1L).getPatient().getId()).isEqualTo(6L);
		assertThat(rest.puts).isEmpty();
	}

	@Test
	void slotCapacityIsChangedWithTheServiceTokenNeverThePatientsToken() throws Exception {
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		AppointmentDto booked = service.createAppointment(booking(null, 3L), AUTH);
		service.cancelAppointment(booked.getId(), AUTH);

		assertThat(rest.puts).hasSize(2); // decrement on booking, increment on cancelling
		assertThat(rest.putAuth).containsOnly("Bearer service-token").doesNotContain(AUTH);
	}

	@Test
	void patientCannotBookForSomeoneElse() {
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		assertThatThrownBy(() -> service.createAppointment(booking(6L, 3L), AUTH))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(store).isEmpty();
		assertThat(rest.puts).isEmpty();
	}

	@Test
	void doctorMayBookForAPatientButNeedsAPatientId() throws Exception {
		loginAs("dr@careplus.com", "ROLE_DOCTOR");

		AppointmentDto saved = service.createAppointment(booking(6L, 3L), AUTH);
		assertThat(store.get(saved.getId()).getPatient().getId()).isEqualTo(6L);

		assertThatThrownBy(() -> service.createAppointment(booking(null, 3L), AUTH))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("patient_id is required");
	}

	@Test
	void bookingRequiresDoctorDateAndTime() {
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;
		AppointmentDto dto = new AppointmentDto();

		assertThatThrownBy(() -> service.createAppointment(dto, AUTH)).isInstanceOf(ResponseStatusException.class);
	}

	// ---- viewing ----

	@Test
	void patientSeesOnlyTheirOwnAppointment() {
		store.put(1L, appointment(1L, 5L, 3L, Status.BOOKED));
		store.put(2L, appointment(2L, 6L, 3L, Status.BOOKED));
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		assertThat(service.getAppointment(1L, AUTH).getPatient_id()).isEqualTo(5L);
		assertThatThrownBy(() -> service.getAppointment(2L, AUTH)).isInstanceOf(AccessDeniedException.class);
		// An unknown id is refused the same way, so ids cannot be probed.
		assertThatThrownBy(() -> service.getAppointment(999L, AUTH)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void doctorAndAdminSeeAnyAppointmentAndUnknownIdIsNull() {
		store.put(2L, appointment(2L, 6L, 3L, Status.BOOKED));
		loginAs("dr@careplus.com", "ROLE_DOCTOR");

		assertThat(service.getAppointment(2L, AUTH).getPatient_id()).isEqualTo(6L);
		assertThat(service.getAppointment(999L, AUTH)).isNull();
	}

	@Test
	void patientWithNoPatientRecordIsRefused() {
		store.put(1L, appointment(1L, 5L, 3L, Status.BOOKED));
		loginAs("nobody@example.com", "ROLE_PATIENT");
		rest.myPatientId = null; // patient-service answers 404 for /me

		assertThatThrownBy(() -> service.getAppointment(1L, AUTH)).isInstanceOf(AccessDeniedException.class);
	}

	// ---- cancelling ----

	@Test
	void patientCancelsOwnBookedAppointmentAndTheSlotIsGivenBack() throws Exception {
		store.put(1L, appointment(1L, 5L, 3L, Status.BOOKED));
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		Appointment cancelled = service.cancelAppointment(1L, AUTH);

		assertThat(cancelled.getStatus()).isEqualTo(Status.CANCELLED);
		assertThat(store.get(1L).getStatus()).isEqualTo(Status.CANCELLED);
		assertThat(rest.puts).containsExactly("http://doctors/api/doctors/3/availability/7/increment");
	}

	@Test
	void patientCannotCancelSomeoneElsesAppointment() throws Exception {
		store.put(2L, appointment(2L, 6L, 3L, Status.BOOKED));
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		assertThatThrownBy(() -> service.cancelAppointment(2L, AUTH)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> service.cancelAppointment(999L, AUTH)).isInstanceOf(AccessDeniedException.class);
		assertThat(store.get(2L).getStatus()).isEqualTo(Status.BOOKED);
		assertThat(rest.puts).isEmpty();
	}

	@Test
	void onlyABookedAppointmentCanBeCancelled() {
		store.put(1L, appointment(1L, 5L, 3L, Status.CANCELLED));
		store.put(2L, appointment(2L, 5L, 3L, Status.COMPLETED));
		loginAs("ada@example.com", "ROLE_PATIENT");
		rest.myPatientId = 5L;

		for (long id : new long[] { 1L, 2L }) {
			assertThatThrownBy(() -> service.cancelAppointment(id, AUTH))
					.isInstanceOf(ResponseStatusException.class)
					.extracting(e -> ((ResponseStatusException) e).getStatusCode())
					.isEqualTo(HttpStatus.CONFLICT);
		}
		assertThat(rest.puts).isEmpty();
	}

	@Test
	void doctorCancelUnknownIdReturnsNullForA404() throws Exception {
		loginAs("dr@careplus.com", "ROLE_DOCTOR");

		assertThat(service.cancelAppointment(999L, AUTH)).isNull();
	}

	// ---- helpers ----

	private AppointmentDto booking(Long patientId, Long doctorId) {
		AppointmentDto dto = new AppointmentDto();
		dto.setPatient_id(patientId);
		dto.setDoctor_id(doctorId);
		dto.setAppointmentDate(DAY);
		dto.setAppointmentTime(LocalTime.of(10, 30));
		dto.setReason("check-up");
		return dto;
	}

	private Appointment appointment(Long id, Long patientId, Long doctorId, Status status) {
		Appointment a = new Appointment();
		a.setId(id);
		Patient p = new Patient();
		p.setId(patientId);
		Doctor d = new Doctor();
		d.setId(doctorId);
		a.setPatient(p);
		a.setDoctor(d);
		a.setAppointmentDate(DAY);
		a.setAppointmentTime(LocalTime.of(10, 30));
		a.setStatus(status);
		a.setReason("check-up");
		return a;
	}

	private static DoctorAvialability slot(Long id, LocalTime start, LocalTime end) {
		DoctorAvialability s = new DoctorAvialability();
		s.setId(id);
		s.setAvailable_date(DAY);
		s.setStart_time(start);
		s.setEnd_time(end);
		s.setMax_appoinments(4);
		return s;
	}

	private static void loginAs(String username, String role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				username, null, List.of(new SimpleGrantedAuthority(role))));
	}

	private void set(String field, Object value) throws Exception {
		Field f = AppointmentService.class.getDeclaredField(field);
		f.setAccessible(true);
		f.set(service, value);
	}

	/** Plays patient-service and doctor-service for the calls AppointmentService makes. */
	private static class FakeRest extends RestTemplate {
		Long myPatientId;
		final List<DoctorAvialability> slots = new ArrayList<>();
		final List<String> puts = new ArrayList<>();
		final List<String> putAuth = new ArrayList<>();

		@Override
		@SuppressWarnings("unchecked")
		public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> request,
				ParameterizedTypeReference<T> type, Object... vars) {
			assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(AUTH);
			if (url.endsWith("/api/patients/me")) {
				if (myPatientId == null) {
					throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", new HttpHeaders(), new byte[0], null);
				}
				return (ResponseEntity<T>) ResponseEntity.ok(Map.<String, Object>of("id", myPatientId));
			}
			if (url.endsWith("/availability")) {
				return (ResponseEntity<T>) ResponseEntity.ok(slots);
			}
			throw new UnsupportedOperationException(url);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> request,
				Class<T> type, Object... vars) {
			if (method == HttpMethod.PUT) {
				puts.add(url);
				putAuth.add(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
				return ResponseEntity.ok().build();
			}
			if (url.contains("/api/patients/")) {
				Patient p = new Patient();
				p.setId(Long.valueOf(url.substring(url.lastIndexOf('/') + 1)));
				return (ResponseEntity<T>) ResponseEntity.ok(p);
			}
			if (url.contains("/api/doctors/")) {
				Doctor d = new Doctor();
				d.setId(Long.valueOf(url.substring(url.lastIndexOf('/') + 1)));
				return (ResponseEntity<T>) ResponseEntity.ok(d);
			}
			throw new UnsupportedOperationException(url);
		}
	}
}
