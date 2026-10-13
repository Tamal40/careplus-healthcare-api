package com.careplus.appointment.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.careplus.appointment.exception.AppointmentExistsException;
import com.careplus.appointment.exception.DoctorNotAvailableException;
import com.careplus.appointment.model.AppointmentDto;
import com.careplus.appointment.service.AppointmentService;
import com.careplus.common.dto.Appointment;

import org.springframework.http.HttpHeaders;
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
 // TODO: add DTOs, validation, service/repository and business logic.
 
	@Autowired
	private AppointmentService service;
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<AppointmentDto> getAppointment(@PathVariable("id") Long id){
		AppointmentDto p1=service.getAppointment(id);
		if(p1==null) {
			AppointmentDto p=new AppointmentDto();
			return new ResponseEntity<AppointmentDto>(p,HttpStatusCode.valueOf(404));
		}
		return new ResponseEntity<AppointmentDto>(p1,HttpStatusCode.valueOf(200));
	}
	
	@PostMapping
	@PreAuthorize("hasAnyRole('PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<AppointmentDto> createAppointment(@RequestBody AppointmentDto dto,
			 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) throws AppointmentExistsException, DoctorNotAvailableException{
		
		AppointmentDto app=service.createAppointment(dto,authorization);
		return new ResponseEntity<AppointmentDto>(app,HttpStatusCode.valueOf(200));
	}
	
	@PutMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<Appointment> createAppointment(@PathVariable("id") Long id,
			 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) throws AppointmentExistsException, DoctorNotAvailableException{
		
		Appointment app=service.cancelAppointment(id, authorization);
		return new ResponseEntity<Appointment>(app,HttpStatusCode.valueOf(200));
	}
	
	@PutMapping("/{id}/complete")
	@PreAuthorize("hasAnyRole('PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<Appointment> completeAppointment(@PathVariable("id") Long id,
			 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) throws AppointmentExistsException, DoctorNotAvailableException{
		
		Appointment app=service.completeAppointment(id, authorization);
		return new ResponseEntity<Appointment>(app,HttpStatusCode.valueOf(200));
	}
}