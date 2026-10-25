package com.careplus.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.careplus.appointment.exception.AppointmentExistsException;
import com.careplus.appointment.exception.DoctorNotAvailableException;
import com.careplus.appointment.model.AppointmentDto;
import com.careplus.appointment.repository.AppointmentRepository;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.Status;

import jakarta.persistence.EntityManager;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.DoctorAvialability;
import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
public class AppointmentService {

	@Value("${services.patient.url:http://localhost:8081}")
private String patientServiceUrl;

@Value("${services.doctor.url:http://localhost:8082}")
private String doctorServiceUrl;
	
	@Autowired
	private AppointmentRepository repo;
	
	@Autowired
	private RestTemplate template;
	
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JwtService jwtService;
	
	@Transactional
	public AppointmentDto getAppointment(Long id) {
		Appointment app=repo.findById(id).orElse(null);
		if(app==null) {
			return null;
		}
		AppointmentDto dto=new AppointmentDto();
		dto.setAppointmentDate(app.getAppointmentDate());
		dto.setAppointmentTime(app.getAppointmentTime());
		dto.setCreated_at(app.getCreated_at());
		dto.setUpdated_at(app.getUpdated_at());
		dto.setReason(app.getReason());
		dto.setStatus(app.getStatus().toString());
		dto.setDoctor_id(app.getDoctor().getId());
		dto.setPatient_id(app.getPatient().getId());
		dto.setId(id);
		return dto;
	}
	
	/**
	 * Same as {@link #getAppointment(Long)}, but a PATIENT may only view their own appointment. An unknown id is
	 * refused for a patient exactly like someone else's id, so ids cannot be probed.
	 */
	@Transactional
	public AppointmentDto getAppointment(Long id, String auth) {
		AppointmentDto dto = getAppointment(id);
		if (callerIsPatient()) {
			Long myId = currentPatientId(auth);
			if (dto == null || !myId.equals(dto.getPatient_id())) {
				throw new AccessDeniedException("You can only view your own appointments");
			}
		}
		return dto;
	}

	/**
	 * Cancels a BOOKED appointment and gives its slot back to the doctor's availability. A PATIENT may cancel only
	 * their own appointment. Returns null for an unknown id (the controller answers 404); a patient gets "access
	 * denied" instead, so ids cannot be probed.
	 */
	@Transactional
	public Appointment cancelAppointment(Long id,String auth) throws DoctorNotAvailableException {
		boolean patientCaller = callerIsPatient();
		Appointment app=repo.findById(id).orElse(null);
		if(app==null) {
			if (patientCaller) {
				throw new AccessDeniedException("You can only cancel your own appointments");
			}
			return app;
		}
		if (patientCaller && !currentPatientId(auth).equals(app.getPatient().getId())) {
			throw new AccessDeniedException("You can only cancel your own appointments");
		}
		if (app.getStatus() != Status.BOOKED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a BOOKED appointment can be cancelled");
		}
		app.setStatus(Status.CANCELLED);
		app.setUpdated_at(LocalDateTime.now());
		repo.saveAndFlush(app);
		releaseSlot(app, auth);
		return app;
	}

	/** Puts one place back on the availability slot that covers this appointment (the reverse of booking). */
	private void releaseSlot(Appointment app, String auth) {
		Long doctorId = app.getDoctor().getId();
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, auth);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		List<DoctorAvialability> availability = template.exchange(
				doctorServiceUrl + "/api/doctors/" + doctorId + "/availability",
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<List<DoctorAvialability>>() {}
		).getBody();
		if (availability == null) {
			return;
		}
		for (DoctorAvialability avail : availability) {
			if (avail.getAvailable_date().isEqual(app.getAppointmentDate())
					&& avail.getStart_time().isBefore(app.getAppointmentTime())
					&& avail.getEnd_time().isAfter(app.getAppointmentTime())) {
				template.exchange(
						doctorServiceUrl + "/api/doctors/" + doctorId + "/availability/" + avail.getId() + "/increment",
						HttpMethod.PUT,
						serviceEntity(),
						Void.class
				);
				return;
			}
		}
	}

	/**
	 * Request entity for changing a doctor's slot capacity. It carries this service's own token, not the caller's:
	 * patients may not change capacity, only booking and cancelling here may.
	 */
	private HttpEntity<Void> serviceEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken());
		return new HttpEntity<>(headers);
	}

	private static boolean callerIsPatient() {
		Authentication caller = SecurityContextHolder.getContext().getAuthentication();
		return caller != null && caller.getAuthorities().stream().anyMatch(a -> "ROLE_PATIENT".equals(a.getAuthority()));
	}

	/** The id of the patient record behind the caller's JWT, asked of patient-service (GET /api/patients/me). */
	private Long currentPatientId(String auth) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, auth);
		Map<String, Object> me;
		try {
			me = template.exchange(
					patientServiceUrl + "/api/patients/me",
					HttpMethod.GET,
					new HttpEntity<Void>(headers),
					new ParameterizedTypeReference<Map<String, Object>>() {}
			).getBody();
		} catch (HttpClientErrorException.NotFound ex) {
			throw new AccessDeniedException("No patient record is linked to this login");
		}
		if (me == null || !(me.get("id") instanceof Number id)) {
			throw new AccessDeniedException("No patient record is linked to this login");
		}
		return id.longValue();
	}
	
	@Transactional
	public Appointment completeAppointment(Long id,String auth) throws DoctorNotAvailableException {
		Appointment app=repo.findById(id).orElse(null);
		if(app==null) {
			return app;
		}
		else {
			app.setStatus(Status.valueOf("COMPLETED"));
			repo.saveAndFlush(app);
		}
		return app;
	}
	
	@Transactional
	public AppointmentDto createAppointment(AppointmentDto p,String auth) throws AppointmentExistsException, DoctorNotAvailableException {
		if (p.getDoctor_id() == null || p.getAppointmentDate() == null || p.getAppointmentTime() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"doctor_id, appointmentDate and appointmentTime are required");
		}
		if (callerIsPatient()) {
			// A patient books only for themselves: the patient comes from the login, never from the request body.
			Long myId = currentPatientId(auth);
			if (p.getPatient_id() != null && !p.getPatient_id().equals(myId)) {
				throw new AccessDeniedException("You can only book appointments for yourself");
			}
			p.setPatient_id(myId);
		} else if (p.getPatient_id() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient_id is required");
		}
		// A new appointment is always BOOKED; the client does not choose the status.
		p.setStatus(Status.BOOKED.name());

		synchronized(this) {
			// A slot is taken unless its appointment was cancelled. The table has a unique key on
			// (doctor, date, time), so a cancelled row cannot be left in place and a second row inserted:
			// the cancelled row is reused for the new booking below.
			Appointment cancelledSlot = null;
			Appointment inSlot = repo.findByDoctor_IdAndAppointmentDateAndAppointmentTime(
					p.getDoctor_id(), p.getAppointmentDate(), p.getAppointmentTime()).orElse(null);
			if (inSlot != null) {
				if (inSlot.getStatus() != Status.CANCELLED) {
					throw new AppointmentExistsException("Appointment slot already booked");
				}
				cancelledSlot = inSlot;
			}
			HttpHeaders headers=new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, auth);
			HttpEntity<Void> entity=new HttpEntity<>(headers);
			String url="";
			Patient pa = template.exchange(
        patientServiceUrl + "/api/patients/" + p.getPatient_id(),
        HttpMethod.GET,
        entity,
        Patient.class
).getBody();
			Doctor doc = template.exchange(
        doctorServiceUrl + "/api/doctors/" + p.getDoctor_id(),
        HttpMethod.GET,
        entity,
        Doctor.class
).getBody();
			List<DoctorAvialability> availability = template.exchange(
        doctorServiceUrl + "/api/doctors/" + p.getDoctor_id() + "/availability",
        HttpMethod.GET,
        entity,
        new ParameterizedTypeReference<List<DoctorAvialability>>() {}
).getBody();
			if(availability==null) {
				throw new DoctorNotAvailableException("Doctor is not available");
			}
			int flag=0;
			for(DoctorAvialability avail:availability) {
				if(!avail.getAvailable_date().isEqual(p.getAppointmentDate())) {
					continue;
				}
				System.out.println(p.getAppointmentTime());
				System.out.println(avail.getStart_time());
				System.out.println(avail.getEnd_time());
				
				if(avail.getStart_time().isBefore(p.getAppointmentTime()) &&
						avail.getEnd_time().isAfter(p.getAppointmentTime())) {
					flag=1;
					 url =
        doctorServiceUrl
        + "/api/doctors/"
        + p.getDoctor_id()
        + "/availability/"
        + avail.getId()
        + "/decrement";

					
					break;
				}
			
			}
			if(flag==0) {
				throw new DoctorNotAvailableException("Doctor is not available");
			}
			
			Appointment app = cancelledSlot != null ? cancelledSlot : new Appointment();
			app.setAppointmentDate(p.getAppointmentDate());
			app.setAppointmentTime(p.getAppointmentTime());
			app.setCreated_at(LocalDateTime.now());
			app.setUpdated_at(LocalDateTime.now());
			app.setReason(p.getReason());
			app.setStatus(Status.valueOf(p.getStatus()));
			app.setDoctor(entityManager.getReference(Doctor.class,p.getDoctor_id()));
			app.setPatient(entityManager.getReference(Patient.class,p.getPatient_id()));
			repo.saveAndFlush(app);
			template.exchange(
			        url,
			        HttpMethod.PUT,
			        serviceEntity(),
			        Void.class
			);
			p.setId(app.getId());
			return p;
			
		}
	}

}