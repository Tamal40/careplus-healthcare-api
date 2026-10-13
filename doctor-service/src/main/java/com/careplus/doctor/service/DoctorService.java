package com.careplus.doctor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.Patient;
import com.careplus.doctor.exception.DoctorExistsException;
import com.careplus.doctor.repository.DoctorRepository;


@Service
@Transactional
public class DoctorService {
	
	@Autowired
	DoctorRepository repo;
	
	@Transactional
	public List<Doctor> getAllDoctor(){
		return repo.findAll();
	}

	@Transactional
	public Doctor getDoctor(Long id) {
		
		return repo.findById(id).orElse(null);
	}
	
@Transactional
public Doctor createDoctor(Doctor p) throws DoctorExistsException {
	    if(repo.findByEmail(p.getEmail()).isPresent()) {
	    	throw new DoctorExistsException("Patient already exists");
	    }
		p.setCreated_at(LocalDate.now());
		return repo.saveAndFlush(p);
	}

@Transactional
public List<AppointmentDto> getAppointments(Long id) {
	Doctor p=repo.findById(id).orElse(null);
	if(p==null) {
		return null;
	}
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

	
}
