package com.careplus.appointment.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.careplus.appointment.model.AppointmentDto;

@RestControllerAdvice
public class AppointmentExceptionHandler {
	
	@ExceptionHandler(AppointmentExistsException.class)
	public ResponseEntity<Map<String, String>> PatientExist() {
		AppointmentDto p=new AppointmentDto();
		return ResponseEntity.status(HttpStatus.CONFLICT).
				body(Map.of("ERROR", "Appointment slot not available"));
	}
	
	@ExceptionHandler(DoctorNotAvailableException.class)
	public ResponseEntity<Map<String, String>> DoctorNotAvailable() {
		AppointmentDto p=new AppointmentDto();
		return ResponseEntity.status(HttpStatus.NOT_FOUND).
				body(Map.of("ERROR", "DOCTOR NOT FOUND"));
	}

}
