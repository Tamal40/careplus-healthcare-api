package com.careplus.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.Appointment;


@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	/**
	 * The appointment occupying a doctor's slot, whatever its status. At most one exists: the table has a unique
	 * key on (doctor, date, time), so a cancelled appointment still holds its slot's row.
	 */
	Optional<Appointment> findByDoctor_IdAndAppointmentDateAndAppointmentTime(
			Long doctorId, LocalDate date, LocalTime time);

}
