package com.careplus.doctor.controller;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.DoctorAvialability;
import com.careplus.doctor.exception.DoctorExistsException;
import com.careplus.doctor.exception.DoctorNotAvailableException;
import com.careplus.doctor.model.DoctorAvailable;
import com.careplus.doctor.service.DoctorAvailabilityService;
import com.careplus.doctor.service.DoctorService;
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
 
	@Autowired
	private DoctorService service;
	@Autowired
	private DoctorAvailabilityService availabilityService;
	
	
	
	@GetMapping
	@PreAuthorize("hasAnyRole( 'DOCTOR', 'ADMIN')")
	public List<Doctor> getAllDoctor(){
		return service.getAllDoctor();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
	public ResponseEntity<Doctor> getDoctor(@PathVariable("id") Long id){
		Doctor p1=service.getDoctor(id);
		if(p1==null) {
			Doctor p=new Doctor();
			return new ResponseEntity<Doctor>(p,HttpStatusCode.valueOf(404));
		}
		return new ResponseEntity<Doctor>(p1,HttpStatusCode.valueOf(200));
	}
	
	@PostMapping("/create")
	@PreAuthorize("hasAnyRole( 'DOCTOR', 'ADMIN')")
	public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doctor) throws DoctorExistsException{
		Doctor p=service.createDoctor(doctor);
		return new ResponseEntity<Doctor>(p,HttpStatusCode.valueOf(201));
	}
	
	@GetMapping("/{id}/availability")
	@PreAuthorize("hasAnyRole( 'PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<List<DoctorAvailable>> getDoctorAvailability(@PathVariable("id") Long id) throws DoctorExistsException, DoctorNotAvailableException{
		Doctor p1=service.getDoctor(id);
		if(p1==null) {
			List<DoctorAvailable> p=new ArrayList<DoctorAvailable>();
			return new ResponseEntity<List<DoctorAvailable>>(p,HttpStatusCode.valueOf(404));
		}
		List<DoctorAvailable> availabilities=availabilityService.getAvailabilityByDoctorId(id);
		return new ResponseEntity<List<DoctorAvailable>>(availabilities,HttpStatusCode.valueOf(200));
		
		
	}
	
	@PostMapping("/{id}/availability")
	@PreAuthorize("hasAnyRole( 'PATIENT','DOCTOR', 'ADMIN')")
	public ResponseEntity<DoctorAvailable> createDoctorAvailability(@PathVariable("id") Long id,@RequestBody DoctorAvialability avail) throws DoctorExistsException, DoctorNotAvailableException{
		Doctor p1=service.getDoctor(id);
		if(p1==null) {
			DoctorAvailable p=new DoctorAvailable();
			return new ResponseEntity<DoctorAvailable>(p,HttpStatusCode.valueOf(404));
		}
		DoctorAvailable availabilities=availabilityService.createAvailability(avail,id);
		return new ResponseEntity<DoctorAvailable>(availabilities,HttpStatusCode.valueOf(200));
		
		
	}
	
	@PutMapping(
		    "/{doctorId}/availability/{availabilityId}/decrement"
		)
	@PreAuthorize("hasAnyRole( 'PATIENT','DOCTOR', 'ADMIN')")
		public ResponseEntity<Void> decrementAvailability(
		        @PathVariable("doctorId") Long doctorId,
		        @PathVariable("availabilityId") Long availabilityId) {

		availabilityService.decrementMaxAppointments(
		            doctorId,
		            availabilityId
		    );

		    return ResponseEntity.noContent().build();
		}
	@PutMapping(
		    "/{doctorId}/availability/{availabilityId}/increment"
		)
	@PreAuthorize("hasAnyRole( 'PATIENT','DOCTOR', 'ADMIN')")
		public ResponseEntity<Void> incrementAvailability(
		        @PathVariable("doctorId") Long doctorId,
		        @PathVariable("availabilityId") Long availabilityId) {

		availabilityService.incrementMaxAppointments(
		            doctorId,
		            availabilityId
		    );

		    return ResponseEntity.noContent().build();
		}
	
	@GetMapping("/{id}/appointments")
	@PreAuthorize("hasAnyRole( 'DOCTOR', 'ADMIN')")
	public ResponseEntity<List<AppointmentDto>> getAppointment(@PathVariable("id") Long id) {
		List<AppointmentDto> p=service.getAppointments(id);
		if(p==null) {
			return new ResponseEntity<List<AppointmentDto>>(p,HttpStatusCode.valueOf(404));
		}
		return new ResponseEntity<List<AppointmentDto>>(p,HttpStatusCode.valueOf(201));
	}
}