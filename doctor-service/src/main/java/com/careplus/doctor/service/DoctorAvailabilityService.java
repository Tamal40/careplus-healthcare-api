package com.careplus.doctor.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.DoctorAvialability;
import com.careplus.doctor.exception.DoctorExistsException;
import com.careplus.doctor.exception.DoctorNotAvailableException;
import com.careplus.doctor.model.DoctorAvailable;
import com.careplus.doctor.repository.DoctorAvailabilityRepository;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class DoctorAvailabilityService {
	
	@Autowired
	private DoctorAvailabilityRepository repo;
	
	@Autowired
	private EntityManager entityManager;
	
	@Transactional
	public List<DoctorAvailable> getAvailabilityByDoctorId(Long doctor_id) throws DoctorNotAvailableException{
		List<DoctorAvialability> availabilities= repo.findByDoctor_Id(doctor_id);
		if(availabilities==null) {
			throw new DoctorNotAvailableException("Doctor is not avialble");
		}
		List<DoctorAvailable> avail=new ArrayList<>();
		for(DoctorAvialability d: availabilities) {
			DoctorAvailable avai=new DoctorAvailable();
			avai.setId(d.getId());
			avai.setAvailable_date(d.getAvailable_date());
			avai.setEnd_time(d.getEnd_time());
			avai.setMax_appoinments(d.getMax_appoinments());
			avai.setStart_time(d.getStart_time());
			avail.add(avai);
		}
		return avail;
	}
	
	@Transactional
	public synchronized void decrementMaxAppointments(
	        Long doctorId,
	        Long availabilityId) {

		DoctorAvialability availability =
	            repo.findById(availabilityId)
	                    .orElseThrow(() ->
	                            new RuntimeException("Availability not found"));

	    if (!availability.getDoctor_id().getId().equals(doctorId)) {
	        throw new RuntimeException(
	                "Availability does not belong to this doctor"
	        );
	    }

	    if (availability.getMax_appoinments() <= 0) {
	        throw new RuntimeException("No appointments available");
	    }

	    availability.setMax_appoinments(
	            availability.getMax_appoinments() - 1
	    );

	    repo.save(availability);
	    System.out.println("Decremented");
	}

	@Transactional
	public void incrementMaxAppointments(Long doctorId, Long availabilityId) {
		DoctorAvialability availability =
	            repo.findById(availabilityId)
	                    .orElseThrow(() ->
	                            new RuntimeException("Availability not found"));

	    if (!availability.getDoctor_id().getId().equals(doctorId)) {
	        throw new RuntimeException(
	                "Availability does not belong to this doctor"
	        );
	    }

	    if (availability.getMax_appoinments() <= 0) {
	        throw new RuntimeException("No appointments available");
	    }

	    availability.setMax_appoinments(
	            availability.getMax_appoinments() + 1
	    );

	    repo.save(availability);
		
	}

	public DoctorAvailable createAvailability(DoctorAvialability avail,Long id) {
		List<DoctorAvialability> result=repo.findByDoctor_Id(id);
		int flag=0;
		for(DoctorAvialability res:result) {
			  if (!avail.getAvailable_date().isEqual(res.getAvailable_date())) {
			        continue;
			    }

			    boolean overlaps =
			            avail.getStart_time().isBefore(res.getEnd_time())
			            &&
			            avail.getEnd_time().isAfter(res.getStart_time());

			    if (overlaps) {
			        flag = 1;
			        break;
			    }
		}
		if(flag==1) {
			 throw new RuntimeException(
		                "Availability already belong to this doctor"
		        );
		}
		avail.setDoctor_id(entityManager.getReference(Doctor.class,id));
		repo.save(avail);
		DoctorAvailable ava=new DoctorAvailable();
		ava.setAvailable_date(avail.getAvailable_date());
		ava.setEnd_time(avail.getEnd_time());
		ava.setStart_time(avail.getStart_time());
		ava.setMax_appoinments(avail.getMax_appoinments());
		return ava;
	}

}
