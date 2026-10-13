package com.careplus.patient.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.service.PatientService;
import org.springframework.http.HttpHeaders;
import com.careplus.common.dto.*;
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
		Patient p1=service.getPatient(id);
		if(p1==null) {
			Patient p=new Patient();
			return new ResponseEntity<Patient>(p,HttpStatusCode.valueOf(404));
		}
		return new ResponseEntity<Patient>(p1,HttpStatusCode.valueOf(200));
	}
	
	@PostMapping("/create")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) throws PatientExistsException{
		Patient p=service.createPatient(patient);
		return new ResponseEntity<Patient>(p,HttpStatusCode.valueOf(201));
	}
	
	@GetMapping("/{id}/appointments")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<List<AppointmentDto>> getAppointment(@PathVariable("id") Long id) throws PatientExistsException{
		List<AppointmentDto> p=service.getAppointments(id);
		if(p==null) {
			return new ResponseEntity<List<AppointmentDto>>(p,HttpStatusCode.valueOf(404));
		}
		return new ResponseEntity<List<AppointmentDto>>(p,HttpStatusCode.valueOf(201));
	}
	
	@PostMapping("/medical-records")
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public ResponseEntity<MedicalRecordRequest> createMedicalRecord(@RequestBody MedicalRecordRequest record
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