package com.careplus.patient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.MedicalRecord;
import com.careplus.common.dto.Prescription;
import com.careplus.common.dto.Status;
import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.dto.PrescriptionRequest;
import com.careplus.patient.exception.AppointmentNotFoundException;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.exception.PatientNotFoundException;
import com.careplus.patient.repository.MedicalRecordRepository;
import com.careplus.patient.repository.PatientRepository;
import com.careplus.patient.repository.PrescriptionRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

	@Mock
	private PatientRepository repo;

	@Mock
	private MedicalRecordRepository medicalrepo;

	@Mock
	private PrescriptionRepository prescriptionrepo;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private PatientService service;

	@Test
	void getPatientReturnsTheStoredPatient() {
		Patient stored = patient("ada@example.com");
		when(repo.findById(1L)).thenReturn(Optional.of(stored));

		assertThat(service.getPatient(1L)).isSameAs(stored);
	}

	@Test
	void getPatientThrowsNotFoundForUnknownId() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPatient(99L))
				.isInstanceOf(PatientNotFoundException.class)
				.hasMessage("Patient 99 not found");
	}

	@Test
	void createPatientRejectsDuplicateEmailWithoutSaving() {
		Patient incoming = patient("ada@example.com");
		when(repo.findByEmail("ada@example.com")).thenReturn(Optional.of(patient("ada@example.com")));

		assertThatThrownBy(() -> service.createPatient(incoming)).isInstanceOf(PatientExistsException.class);
		verify(repo, never()).saveAndFlush(any());
	}

	@Test
	void createPatientStampsCreatedAndUpdatedDates() throws Exception {
		Patient incoming = patient("grace@example.com");
		when(repo.findByEmail("grace@example.com")).thenReturn(Optional.empty());
		when(repo.saveAndFlush(incoming)).thenReturn(incoming);

		Patient saved = service.createPatient(incoming);

		assertThat(saved.getCreated_at()).isEqualTo(LocalDate.now());
		assertThat(saved.getUpdated_at()).isEqualTo(LocalDate.now());
	}

	@Test
	void getAppointmentsMapsEachAppointment() {
		Patient stored = patient("ada@example.com");
		stored.setId(1L);
		Appointment appointment = new Appointment();
		appointment.setId(11L);
		Doctor doctor = new Doctor();
		doctor.setId(3L);
		appointment.setDoctor(doctor);
		appointment.setAppointmentDate(LocalDate.of(2026, 11, 2));
		appointment.setAppointmentTime(LocalTime.of(10, 30));
		appointment.setReason("Follow-up");
		appointment.setStatus(Status.BOOKED);
		appointment.setCreated_at(LocalDateTime.of(2026, 10, 1, 9, 0));
		stored.setAppointments(List.of(appointment));
		when(repo.findById(1L)).thenReturn(Optional.of(stored));

		List<AppointmentDto> result = service.getAppointments(1L);

		assertThat(result).hasSize(1);
		AppointmentDto dto = result.get(0);
		// id and doctor_id let a client pick one appointment (e.g. to cancel it).
		assertThat(dto.getId()).isEqualTo(11L);
		assertThat(dto.getDoctor_id()).isEqualTo(3L);
		assertThat(dto.getPatient_id()).isEqualTo(1L);
		assertThat(dto.getAppointmentDate()).isEqualTo(LocalDate.of(2026, 11, 2));
		assertThat(dto.getAppointmentTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(dto.getReason()).isEqualTo("Follow-up");
		assertThat(dto.getStatus()).isEqualTo("BOOKED");
		assertThat(dto.getCreated_at()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
	}

	@Test
	void getAppointmentsToleratesAppointmentWithoutStatus() {
		Patient stored = patient("ada@example.com");
		Appointment appointment = new Appointment();
		appointment.setReason("Walk-in");
		stored.setAppointments(List.of(appointment));
		when(repo.findById(1L)).thenReturn(Optional.of(stored));

		List<AppointmentDto> result = service.getAppointments(1L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getStatus()).isNull();
	}

	@Test
	void createRecordWithoutPrescriptionsSavesOnlyTheRecord() {
		stubAppointmentLookup(7L);
		MedicalRecordRequest request = new MedicalRecordRequest();
		request.setAppointment_id(7L);
		request.setDiagnosis("Seasonal allergy");

		MedicalRecordRequest result = service.createRecord(request, "Bearer token");

		assertThat(result).isSameAs(request);
		verify(medicalrepo).save(any(MedicalRecord.class));
		verify(prescriptionrepo, never()).save(any());
	}

	@Test
	void createRecordLinksRecordToTheAppointmentsPatientAndDoctor() {
		stubAppointmentLookup(7L);
		MedicalRecordRequest request = new MedicalRecordRequest();
		request.setAppointment_id(7L);
		request.setDiagnosis("Infection");
		request.setNotes("Rest for a week");

		service.createRecord(request, "Bearer token");

		// Ids come from the AppointmentDto: appointment 7, patient 1, doctor 2. Swapping them must fail.
		verify(entityManager).getReference(Appointment.class, 7L);
		verify(entityManager).getReference(Patient.class, 1L);
		verify(entityManager).getReference(Doctor.class, 2L);
		ArgumentCaptor<MedicalRecord> saved = ArgumentCaptor.forClass(MedicalRecord.class);
		verify(medicalrepo).save(saved.capture());
		assertThat(saved.getValue().getDiagnosis()).isEqualTo("Infection");
		assertThat(saved.getValue().getNotes()).isEqualTo("Rest for a week");
	}

	@Test
	void createRecordSavesEachPrescriptionLinkedToTheRecord() {
		stubAppointmentLookup(7L);
		MedicalRecord recordReference = new MedicalRecord();
		// Lenient: createRecord also calls getReference for the appointment, patient and doctor.
		lenient().when(entityManager.getReference(MedicalRecord.class, 50L)).thenReturn(recordReference);
		MedicalRecordRequest request = new MedicalRecordRequest();
		request.setAppointment_id(7L);
		request.setDiagnosis("Infection");
		request.setPrescriptions(List.of(prescription("Amoxicillin"), prescription("Paracetamol")));

		service.createRecord(request, "Bearer token");

		ArgumentCaptor<Prescription> saved = ArgumentCaptor.forClass(Prescription.class);
		verify(prescriptionrepo, times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(Prescription::getMedicine_name).containsExactly("Amoxicillin", "Paracetamol");
		assertThat(saved.getAllValues()).extracting(Prescription::getDosage).containsOnly("500mg");
		assertThat(saved.getAllValues()).extracting(Prescription::getMedicalRecord).containsOnly(recordReference);
	}

	@Test
	void createRecordForwardsTheCallersAuthorizationHeader() {
		stubAppointmentLookup(7L);
		MedicalRecordRequest request = new MedicalRecordRequest();
		request.setAppointment_id(7L);
		request.setDiagnosis("Infection");

		service.createRecord(request, "Bearer caller-token");

		ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<HttpEntity<Void>> entity = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate).exchange(url.capture(), eq(HttpMethod.GET), entity.capture(), eq(AppointmentDto.class));
		assertThat(url.getValue()).endsWith("/api/appointments/7");
		assertThat(entity.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer caller-token");
	}

	@Test
	void getMedicalHistoryMapsNotesAndPrescriptions() {
		Appointment appointment = new Appointment();
		appointment.setId(7L);
		MedicalRecord record = new MedicalRecord();
		record.setId(50L);
		record.setAppointment(appointment);
		record.setDiagnosis("Infection");
		record.setNotes("Rest for a week");
		Prescription prescription = new Prescription();
		prescription.setMedicine_name("Amoxicillin");
		prescription.setDosage("500mg");
		when(medicalrepo.findByPatient_Id(1L)).thenReturn(List.of(record));
		when(prescriptionrepo.findByid(50L)).thenReturn(List.of(prescription));

		List<MedicalRecordRequest> history = service.getMedicalHistory(1L);

		assertThat(history).hasSize(1);
		MedicalRecordRequest entry = history.get(0);
		assertThat(entry.getDiagnosis()).isEqualTo("Infection");
		// Notes used to be filled from the diagnosis.
		assertThat(entry.getNotes()).isEqualTo("Rest for a week");
		assertThat(entry.getAppointment_id()).isEqualTo(7L);
		assertThat(entry.getPrescriptions()).extracting(PrescriptionRequest::getMedicine_name).containsExactly("Amoxicillin");
	}

	@Test
	void getMedicalHistoryForPatientWithoutRecordsIsEmpty() {
		when(medicalrepo.findByPatient_Id(1L)).thenReturn(List.of());

		assertThat(service.getMedicalHistory(1L)).isEmpty();
	}

	@Test
	void createRecordForUnknownAppointmentThrowsNotFound() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AppointmentDto.class)))
				.thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));
		MedicalRecordRequest request = new MedicalRecordRequest();
		request.setAppointment_id(404L);
		request.setDiagnosis("n/a");

		assertThatThrownBy(() -> service.createRecord(request, "Bearer token"))
				.isInstanceOf(AppointmentNotFoundException.class)
				.hasMessage("Appointment 404 not found");
		verify(medicalrepo, never()).save(any());
	}

	private void stubAppointmentLookup(Long appointmentId) {
		AppointmentDto appointment = new AppointmentDto();
		appointment.setId(appointmentId);
		appointment.setPatient_id(1L);
		appointment.setDoctor_id(2L);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AppointmentDto.class)))
				.thenReturn(ResponseEntity.ok(appointment));
		MedicalRecord saved = new MedicalRecord();
		saved.setId(50L);
		when(medicalrepo.save(any(MedicalRecord.class))).thenReturn(saved);
	}

	private static PrescriptionRequest prescription(String medicine) {
		PrescriptionRequest p = new PrescriptionRequest();
		p.setMedicine_name(medicine);
		p.setDosage("500mg");
		return p;
	}

	@Test
	void getAppointmentsThrowsNotFoundForUnknownPatient() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAppointments(99L)).isInstanceOf(PatientNotFoundException.class);
	}

	@Test
	void getPatientByEmailReturnsThePatientBehindTheLogin() {
		Patient stored = patient("ada@example.com");
		when(repo.findByEmail("ada@example.com")).thenReturn(Optional.of(stored));

		assertThat(service.getPatientByEmail(" ada@example.com ")).isSameAs(stored);
	}

	@Test
	void getPatientByEmailThrowsNotFoundWhenNoPatientHasThatEmail() {
		when(repo.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPatientByEmail("nobody@example.com"))
				.isInstanceOf(PatientNotFoundException.class);
		assertThatThrownBy(() -> service.getPatientByEmail(" "))
				.isInstanceOf(PatientNotFoundException.class);
	}

	@Test
	void searchPatientsConvertsAgeRangeToBirthDateRange() {
		LocalDate today = LocalDate.now();
		when(repo.searchPatients(any(), any(), any(), any(), eq(0), eq(10))).thenReturn(List.of());

		service.searchPatients("female", "  ada ", 50, 60, 0, 10);

		// age >= 50: born on/before today-50y. age <= 60: born after today-61y.
		verify(repo).searchPatients("FEMALE", "ada", today.minusYears(61).plusDays(1), today.minusYears(50), 0, 10);
	}

	@Test
	void searchPatientsPassesNullsWhenFiltersAreMissing() {
		when(repo.searchPatients(any(), any(), any(), any(), eq(0), eq(10))).thenReturn(List.of());

		service.searchPatients(null, " ", null, null, 0, 10);

		verify(repo).searchPatients(null, null, null, null, 0, 10);
	}

	@Test
	void searchPatientsRejectsInvalidFilters() {
		assertThatThrownBy(() -> service.searchPatients("other", null, null, null, 0, 10))
				.isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
		assertThatThrownBy(() -> service.searchPatients(null, null, 60, 50, 0, 10))
				.isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
		assertThatThrownBy(() -> service.searchPatients(null, null, -1, null, 0, 10))
				.isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
		verify(repo, never()).searchPatients(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
	}

	private static Patient patient(String email) {
		Patient p = new Patient();
		p.setName("Ada Lovelace");
		p.setEmail(email);
		p.setPhone("9000000000");
		p.setDate_of_birth(LocalDate.of(1990, 1, 1));
		return p;
	}
}
