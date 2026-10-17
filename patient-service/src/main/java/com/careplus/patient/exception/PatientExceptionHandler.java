package com.careplus.patient.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Turns patient API failures into an {@link ErrorResponse} with the right status. Errors used to come back as an
 * empty Patient object, so a client saw a 404 or 409 with a body that looked like a blank record and no reason.
 */
@RestControllerAdvice
public class PatientExceptionHandler {

	@ExceptionHandler(PatientExistsException.class)
	public ResponseEntity<ErrorResponse> patientExists(PatientExistsException ex) {
		return error(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(PatientNotFoundException.class)
	public ResponseEntity<ErrorResponse> patientNotFound(PatientNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** Bean Validation failures on a {@code @Valid} request body, reported per field. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> invalidRequest(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status)
				.body(new ErrorResponse(status.value(), status.getReasonPhrase(), "Request validation failed", fieldErrors, Instant.now()));
	}

	private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message));
	}
}
