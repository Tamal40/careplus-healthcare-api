package com.careplus.patient.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.MedicalRecord;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
	
	@Query(value="select * from medical_record where patient_id=:id",nativeQuery=true)
	List<MedicalRecord> findByPatient_Id(@Param("id")Long id);

}
