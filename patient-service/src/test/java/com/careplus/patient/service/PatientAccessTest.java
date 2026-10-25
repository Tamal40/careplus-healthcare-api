package com.careplus.patient.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.careplus.common.dto.Patient;
import com.careplus.patient.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientAccessTest {

	@Mock
	private PatientRepository repo;

	@InjectMocks
	private PatientAccess access;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void patientMayReadTheirOwnRecordRegardlessOfEmailCase() {
		login("Ada@Example.com", "ROLE_PATIENT");
		lenient().when(repo.findById(1L)).thenReturn(Optional.of(patient("ada@example.com")));

		assertThatCode(() -> access.requireAccess(1L)).doesNotThrowAnyException();
	}

	@Test
	void patientCannotReadAnotherPatient() {
		login("ada@example.com", "ROLE_PATIENT");
		lenient().when(repo.findById(2L)).thenReturn(Optional.of(patient("grace@example.com")));

		assertThatThrownBy(() -> access.requireAccess(2L)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void unknownIdIsRefusedForAPatientTheSameWayAsSomeoneElsesId() {
		login("ada@example.com", "ROLE_PATIENT");
		lenient().when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> access.requireAccess(99L)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void doctorAndAdminMayReadAnyPatient() {
		login("dr@careplus.com", "ROLE_DOCTOR");
		assertThatCode(() -> access.requireAccess(2L)).doesNotThrowAnyException();

		login("root", "ROLE_ADMIN");
		assertThatCode(() -> access.requireAccess(2L)).doesNotThrowAnyException();
	}

	@Test
	void nobodySignedInIsRefused() {
		assertThatThrownBy(() -> access.requireAccess(1L)).isInstanceOf(AccessDeniedException.class);
	}

	private static void login(String username, String role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				username, null, List.of(new SimpleGrantedAuthority(role))));
	}

	private static Patient patient(String email) {
		Patient p = new Patient();
		p.setName("Test");
		p.setEmail(email);
		p.setPhone("9000000000");
		p.setDate_of_birth(LocalDate.of(1990, 1, 1));
		return p;
	}
}
