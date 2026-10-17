package com.careplus.patient.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PatientExceptionHandlerTest {

	private final PatientExceptionHandler handler = new PatientExceptionHandler();

	@Test
	void duplicatePatientIsAConflictWithTheReason() {
		ResponseEntity<ErrorResponse> response = handler.patientExists(new PatientExistsException("Patient already exists"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().status()).isEqualTo(409);
		assertThat(response.getBody().message()).isEqualTo("Patient already exists");
	}

	@Test
	void unknownPatientIsNotFoundWithTheId() {
		ResponseEntity<ErrorResponse> response = handler.patientNotFound(new PatientNotFoundException(42L));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().message()).isEqualTo("Patient 42 not found");
		assertThat(response.getBody().fieldErrors()).isEmpty();
	}
}
