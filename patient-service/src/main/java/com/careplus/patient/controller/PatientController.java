package com.careplus.patient.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.service.PatientService;
import org.springframework.http.HttpHeaders;
import com.careplus.common.dto.*;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/patients")
public class PatientController {
	
	@Autowired
	private PatientService service;
	
	
	@GetMapping
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public List<Patient> getAllPatients(){
		return service.getAllPatient();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<Patient> getPatient(@PathVariable("id") Long id){
		// An unknown id throws PatientNotFoundException, which PatientExceptionHandler maps to 404.
		return ResponseEntity.ok(service.getPatient(id));
	}
	
	@PostMapping("/create")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<Patient> createPatient(@Valid @RequestBody Patient patient) throws PatientExistsException{
		Patient p=service.createPatient(patient);
		return ResponseEntity.status(HttpStatus.CREATED).body(p);
	}
	
	@GetMapping("/{id}/appointments")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<List<AppointmentDto>> getAppointment(@PathVariable("id") Long id){
		// A read is 200 OK; this used to answer 201 Created. An unknown patient id is a 404 via the handler.
		return ResponseEntity.ok(service.getAppointments(id));
	}
	
	@PostMapping("/medical-records")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<MedicalRecordRequest> createMedicalRecord(@Valid @RequestBody MedicalRecordRequest record
			,  @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) throws PatientExistsException{
		  MedicalRecordRequest response =
	                service.createRecord(record, auth);

	        return ResponseEntity
	                .status(HttpStatus.CREATED)
	                .body(response);
	}
	
	@GetMapping("/{id}/medical-history")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<List<MedicalRecordRequest>> getMedicalRecord(@PathVariable("id") Long id
			,  @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) throws PatientExistsException{
		  List<MedicalRecordRequest> response =
	                service.getMedicalHistory(id);

	        return ResponseEntity
	                .status(HttpStatus.OK)
	                .body(response);
	}


}