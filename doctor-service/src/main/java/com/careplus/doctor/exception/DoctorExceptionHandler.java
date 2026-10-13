package com.careplus.doctor.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.DoctorAvialability;


@RestControllerAdvice
public class DoctorExceptionHandler {
	
	@ExceptionHandler(DoctorExistsException.class)
	public ResponseEntity<Doctor> PatientExist() {
		Doctor p=new Doctor();
		return new ResponseEntity<>(p,HttpStatusCode.valueOf(409));
	}
	
	@ExceptionHandler(DoctorNotAvailableException.class)
	public ResponseEntity<List<DoctorAvialability>> DoctorNotAvailabale() {
		List<DoctorAvialability> p=new ArrayList<>();
		return new ResponseEntity<>(p,HttpStatusCode.valueOf(404));
	}

}
