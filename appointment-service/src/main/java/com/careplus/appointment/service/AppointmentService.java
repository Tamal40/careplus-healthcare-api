package com.careplus.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
	
	@Transactional
	public Appointment cancelAppointment(Long id,String auth) throws DoctorNotAvailableException {
		Appointment app=repo.findById(id).orElse(null);
		if(app==null) {
			return app;
		}
		else {
			app.setStatus(Status.valueOf("CANCELLED"));
            repo.saveAndFlush(app);
			
		}
		return app;
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
		  
		synchronized(this) {
			Long exists=repo.findByDoctorDateTime(p.getDoctor_id(),p.getAppointmentDate(),p.getAppointmentTime());
			if(exists==1L) {
				throw new AppointmentExistsException("Appointment slot already booked");
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
			
			Appointment app=new Appointment();
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
			        entity,
			        Void.class
			);
			return p;
			
		}
	}

}