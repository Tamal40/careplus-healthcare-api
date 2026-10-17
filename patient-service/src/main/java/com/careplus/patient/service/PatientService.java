package com.careplus.patient.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.MedicalRecord;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.Prescription;
import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.dto.PrescriptionRequest;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.exception.PatientNotFoundException;
import com.careplus.patient.repository.MedicalRecordRepository;
import com.careplus.patient.repository.PatientRepository;
import com.careplus.patient.repository.PrescriptionRepository;

import jakarta.persistence.EntityManager;


@Service
@Transactional
public class PatientService {
	

	@Autowired
	PatientRepository repo;
	
	@Autowired
	private MedicalRecordRepository medicalrepo;
	
	@Autowired
	private PrescriptionRepository prescriptionrepo;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private EntityManager entityManager;
	
	@Transactional
	public List<Patient> getAllPatient(){
		return repo.findAll();
	}

	@Transactional
	public Patient getPatient(Long id) {
		
		return repo.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
	}
	
@Transactional
public Patient createPatient(Patient p) throws PatientExistsException {
	    if(repo.findByEmail(p.getEmail()).isPresent()) {
	    	throw new PatientExistsException("Patient already exists");
	    }
		
	    p.setCreated_at(LocalDate.now());
	    p.setUpdated_at(LocalDate.now());
		return repo.saveAndFlush(p);
	}

@Transactional
public List<AppointmentDto> getAppointments(Long id) {
	Patient p=repo.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
	List<Appointment> app=p.getAppointments();
	List<AppointmentDto> appt=new ArrayList<>();
	for(Appointment a:app) {
		AppointmentDto ap=new AppointmentDto();
		ap.setAppointmentDate(a.getAppointmentDate());
		ap.setAppointmentTime(a.getAppointmentTime());
		ap.setReason(a.getReason());
		ap.setCreated_at(a.getCreated_at());
		ap.setUpdated_at(a.getUpdated_at());
		ap.setStatus(a.getStatus().toString());
		appt.add(ap);
	}
	return appt;
	
}

@Transactional
public MedicalRecordRequest createRecord(MedicalRecordRequest record,String auth) {
	
	HttpHeaders headers=new HttpHeaders();
	headers.set(HttpHeaders.AUTHORIZATION, auth);
	HttpEntity<Void> entity=new HttpEntity<>(headers);
	AppointmentDto appointment = restTemplate.exchange(
	        "http://localhost:8083/api/appointments/"
	                + record.getAppointment_id(),
	        HttpMethod.GET,
	        entity,
	        AppointmentDto.class
	).getBody();

	if (appointment == null) {
	    throw new RuntimeException("Appointment not found");
	}
	MedicalRecord rec = new MedicalRecord();

	rec.setAppointment(entityManager.getReference(Appointment.class,appointment.getId()));
	rec.setPatient(entityManager.getReference(Patient.class,appointment.getPatient_id()));
	rec.setDoctor(entityManager.getReference(Doctor.class,appointment.getDoctor_id()));

	rec.setDiagnosis(record.getDiagnosis());
	rec.setNotes(record.getNotes());
	rec.setCreated_at(LocalDateTime.now());

	MedicalRecord recc=medicalrepo.save(rec);
	
	// Prescriptions are optional: a medical record without any used to throw a NullPointerException here,
	// after the record itself had already been saved.
	List<PrescriptionRequest> prescriptions = record.getPrescriptions() != null ? record.getPrescriptions() : List.of();
	for (PrescriptionRequest p : prescriptions) {

	    Prescription prescription = new Prescription();

	    prescription.setMedicalRecord(entityManager.getReference(MedicalRecord.class,recc.getId()));
	    prescription.setMedicine_name(p.getMedicine_name());
	    prescription.setDosage(p.getDosage());
	    prescription.setFrequency(p.getFrequency());
	    prescription.setDuration(p.getDuration());

	    prescriptionrepo.save(prescription);
	}
	return record;
}

public List<MedicalRecordRequest> getMedicalHistory(Long id) {
	List<MedicalRecord> med=medicalrepo.findByPatient_Id(id);
	if(med==null) {
		throw new RuntimeException("MedicalHistory not found");
	}
	List<MedicalRecordRequest> rec=new ArrayList<>();
	for(MedicalRecord reco:med) {
		MedicalRecordRequest req=new MedicalRecordRequest();
		req.setDiagnosis(reco.getDiagnosis());
		req.setNotes(reco.getDiagnosis());
		req.setAppointment_id(reco.getAppointment().getId());
		List<Prescription> pres=prescriptionrepo.findByid(reco.getId());
		List<PrescriptionRequest> presc=new ArrayList<>();
		if(pres!=null) {
			pres.forEach(p->{
				PrescriptionRequest p1=new PrescriptionRequest();
				p1.setDosage(p.getDosage());
				p1.setDuration(p.getDuration());
				p1.setFrequency(p.getFrequency());
				p1.setMedicine_name(p.getMedicine_name());
				presc.add(p1);
				
			});
		}
		req.setPrescriptions(presc);
		rec.add(req);
		
	}
	return rec;
	
}

}
