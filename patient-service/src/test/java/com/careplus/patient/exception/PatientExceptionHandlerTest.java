package com.careplus.patient.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.careplus.common.dto.Patient;

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

	@Test
	void unknownAppointmentIsNotFound() {
		ResponseEntity<ErrorResponse> response = handler.appointmentNotFound(new AppointmentNotFoundException(7L));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().message()).isEqualTo("Appointment 7 not found");
	}

	@Test
	void validationFailureIsBadRequestWithOneMessagePerField() throws Exception {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Patient(), "patient");
		result.addError(new FieldError("patient", "name", "name is required"));
		result.addError(new FieldError("patient", "email", "email is required"));
		result.addError(new FieldError("patient", "email", "email must be a valid email address"));
		MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("target", Patient.class), 0);

		ResponseEntity<ErrorResponse> response = handler.invalidRequest(new MethodArgumentNotValidException(parameter, result));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().message()).isEqualTo("Request validation failed");
		// The first message per field wins; the second email error is dropped.
		assertThat(response.getBody().fieldErrors())
				.isEqualTo(Map.of("name", "name is required", "email", "email is required"));
	}

	@Test
	void malformedBodyIsBadRequest() {
		ResponseEntity<ErrorResponse> response = handler.unreadableRequest(
				new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0])));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().message()).isEqualTo("Malformed request body");
	}

	@Test
	void downstreamServerErrorIsBadGatewayWithoutItsBody() {
		ResponseEntity<ErrorResponse> response = handler.downstreamServiceError(HttpServerErrorException.create(
				HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY, "stack trace here".getBytes(), null));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getBody().message()).isEqualTo("Downstream service request failed").doesNotContain("stack trace");
	}

	@Test
	void downstreamClientErrorKeepsItsStatus() {
		ResponseEntity<ErrorResponse> response = handler.downstreamServiceError(HttpClientErrorException.create(
				HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody().message()).isEqualTo("Downstream service rejected the request");
	}

	@SuppressWarnings("unused")
	private void target(Patient patient) {
		// Only supplies a MethodParameter for MethodArgumentNotValidException.
	}
}
