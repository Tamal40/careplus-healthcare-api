package com.careplus.patient.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.Patient;


@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
	
	 Optional<Patient> findByEmail(String email);

	 @Query(value="SELECT * FROM patient LIMIT :limit OFFSET :offset",nativeQuery=true)
	 List<Patient> findAllPatients(@Param("offset") int offset,@Param("limit") int limit);

}
