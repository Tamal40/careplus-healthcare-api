package com.careplus.patient.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.Patient;


@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
	
	 Optional<Patient> findByEmail(String email);

}
