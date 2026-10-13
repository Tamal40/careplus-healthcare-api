package com.careplus.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.careplus.common.dto.MedicalRecord;
import com.careplus.common.dto.Prescription;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
	
	@Query(value="select * from prescription where medical_record_id=:id",nativeQuery=true)
	List<Prescription> findByid(@Param("id")Long id);

}
