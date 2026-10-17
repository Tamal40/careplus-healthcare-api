package com.careplus.patient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.Status;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.exception.PatientNotFoundException;
import com.careplus.patient.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

	@Mock
	private PatientRepository repo;

	@InjectMocks
	private PatientService service;

	@Test
	void getPatientReturnsTheStoredPatient() {
		Patient stored = patient("ada@example.com");
		when(repo.findById(1L)).thenReturn(Optional.of(stored));

		assertThat(service.getPatient(1L)).isSameAs(stored);
	}

	@Test
	void getPatientThrowsNotFoundForUnknownId() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPatient(99L))
				.isInstanceOf(PatientNotFoundException.class)
				.hasMessage("Patient 99 not found");
	}

	@Test
	void createPatientRejectsDuplicateEmailWithoutSaving() {
		Patient incoming = patient("ada@example.com");
		when(repo.findByEmail("ada@example.com")).thenReturn(Optional.of(patient("ada@example.com")));

		assertThatThrownBy(() -> service.createPatient(incoming)).isInstanceOf(PatientExistsException.class);
		verify(repo, never()).saveAndFlush(any());
	}

	@Test
	void createPatientStampsCreatedAndUpdatedDates() throws Exception {
		Patient incoming = patient("grace@example.com");
		when(repo.findByEmail("grace@example.com")).thenReturn(Optional.empty());
		when(repo.saveAndFlush(incoming)).thenReturn(incoming);

		Patient saved = service.createPatient(incoming);

		assertThat(saved.getCreated_at()).isEqualTo(LocalDate.now());
		assertThat(saved.getUpdated_at()).isEqualTo(LocalDate.now());
	}

	@Test
	void getAppointmentsMapsEachAppointment() {
		Patient stored = patient("ada@example.com");
		Appointment appointment = new Appointment();
		appointment.setAppointmentDate(LocalDate.of(2026, 11, 2));
		appointment.setAppointmentTime(LocalTime.of(10, 30));
		appointment.setReason("Follow-up");
		appointment.setStatus(Status.BOOKED);
		appointment.setCreated_at(LocalDateTime.of(2026, 10, 1, 9, 0));
		stored.setAppointments(List.of(appointment));
		when(repo.findById(1L)).thenReturn(Optional.of(stored));

		List<AppointmentDto> result = service.getAppointments(1L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getReason()).isEqualTo("Follow-up");
		assertThat(result.get(0).getStatus()).isEqualTo("BOOKED");
		assertThat(result.get(0).getAppointmentTime()).isEqualTo(LocalTime.of(10, 30));
	}

	@Test
	void getAppointmentsThrowsNotFoundForUnknownPatient() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAppointments(99L)).isInstanceOf(PatientNotFoundException.class);
	}

	private static Patient patient(String email) {
		Patient p = new Patient();
		p.setName("Ada Lovelace");
		p.setEmail(email);
		p.setPhone("9000000000");
		p.setDate_of_birth(LocalDate.of(1990, 1, 1));
		return p;
	}
}
