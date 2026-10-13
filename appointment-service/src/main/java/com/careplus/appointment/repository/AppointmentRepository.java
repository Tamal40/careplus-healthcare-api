package com.careplus.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.Appointment;


@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
	
	
	@Query(value="""
			select exists(
			select 1 from appointment where doctor_id=:doctor_id
			and appointment_date=:date 
			and appointment_time = :time)
			""",nativeQuery=true)
	public Long  findByDoctorDateTime(@Param("doctor_id") Long doctor_id
			,@Param("date") LocalDate date,@Param("time") LocalTime time);

}
