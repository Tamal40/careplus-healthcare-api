package com.careplus.patient.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.careplus.common.dto.Patient;


@RestControllerAdvice
public class PatientExceptionHandler {
	
	@ExceptionHandler(PatientExistsException.class)
	public ResponseEntity<Patient> PatientExist() {
		Patient p=new Patient();
		return new ResponseEntity<>(p,HttpStatusCode.valueOf(409));
	}

}
