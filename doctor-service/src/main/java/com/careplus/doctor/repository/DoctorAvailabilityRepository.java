package com.careplus.doctor.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.DoctorAvialability;


@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvialability, Long> {
	
	List<DoctorAvialability> findByDoctor_Id(Long doctorId);
	
	@Query(value="""
			select exists(
			select 1 from doctor_availability where doctor_id=:doctor_id
			and available_date=:date 
			and start_time = :start_time
			and end_time = :end_time)
			""",nativeQuery=true)
	public Long  findByDoctorDateTime(@Param("doctor_id") Long doctor_id
			,@Param("date") LocalDate date,@Param("start_time") LocalTime start_time,@Param("end_time") LocalTime end_time);

}
