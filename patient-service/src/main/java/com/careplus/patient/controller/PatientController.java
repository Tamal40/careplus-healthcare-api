package com.careplus.patient.controller;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.dto.PatientUploadResponse;
import com.careplus.patient.dto.UploadJobResponse;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.service.CsvUploadStatusService;
import com.careplus.patient.service.PatientCsvUploadService;
import com.careplus.patient.service.PatientService;
import org.springframework.http.HttpHeaders;
import com.careplus.common.dto.*;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/patients")
public class PatientController {
	
	@Autowired
	private PatientService service;
	
	@Autowired
	private PatientCsvUploadService uploadService;
	
	@Autowired
	private  CsvUploadStatusService statusService;
	
	
	@GetMapping
	@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
	public List<Patient> getAllPatients(@RequestParam(defaultValue = "0",name = "page") int page,@RequestParam(defaultValue = "10",name = "limit") int limit){
		return service.getAllPatient(page,limit);
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
	
	@PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('DOCTOR')")
	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file ){
		try {

		 UploadJobResponse response = uploadService.submit(file);

	        return ResponseEntity.accepted().body(response);

	    } catch (IllegalArgumentException ex) {
	        return ResponseEntity.badRequest()
	                .body(java.util.Map.of(
	                        "error", ex.getMessage()));
	    } catch (IOException ex) {
	        return ResponseEntity.internalServerError()
	                .body(java.util.Map.of(
	                        "error", "Unable to accept uploaded file"));
	    }
	}
	
	  @GetMapping("/{jobId}/job")
	    public UploadJobStatusResponse getJobStatus(
	            @PathVariable("jobId") Long jobId) {
	        return statusService.getJobStatus(jobId);
	    }

	    @GetMapping("/{jobId}/batches")
	    public List<UploadBatchResponse> getBatches(
	            @PathVariable("jobId") Long jobId) {
	        return statusService.getBatches(jobId);
	    }

	    @GetMapping("/{jobId}/errors")
	    public List<UploadRowErrorResponse> getErrors(
	            @PathVariable("jobId") Long jobId) {
	        return statusService.getErrors(jobId);
	    }


}