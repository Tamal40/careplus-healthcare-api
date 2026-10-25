package com.careplus.patient.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;


/**
 * Turns patient API failures into an {@link ErrorResponse} with the right status. Errors used to come back as an
 * empty Patient object, so a client saw a 404 or 409 with a body that looked like a blank record and no reason.
 */
@RestControllerAdvice
public class PatientExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientExceptionHandler.class);

	@ExceptionHandler(PatientExistsException.class)
	public ResponseEntity<ErrorResponse> patientExists(PatientExistsException ex) {
		return error(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(PatientNotFoundException.class)
	public ResponseEntity<ErrorResponse> patientNotFound(PatientNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AppointmentNotFoundException.class)
	public ResponseEntity<ErrorResponse> appointmentNotFound(AppointmentNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** The caller is signed in but may not see this patient (see PatientAccess). */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex) {
		return error(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	/** A request body that is not valid JSON, or does not fit the target type, e.g. a malformed date. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException ex) {
		return error(HttpStatus.BAD_REQUEST, "Malformed request body");
	}

	/**
	 * A call to another service (appointment-service) failed in a way other than an unknown appointment, which
	 * createRecord turns into AppointmentNotFoundException first. A 4xx keeps its status, because it is usually about
	 * the caller (a forwarded token rejected with 401/403, or a bad id), and only a 5xx is reported as 502. Mapping
	 * everything to 502 made an auth failure look like an outage. The downstream body is never passed through.
	 */
	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<ErrorResponse> downstreamServiceError(RestClientResponseException ex) {
		LOGGER.error("Downstream service call failed with {}", ex.getStatusCode(), ex);
		HttpStatus upstream = HttpStatus.resolve(ex.getStatusCode().value());
		if (upstream != null && upstream.is4xxClientError()) {
			return error(upstream, "Downstream service rejected the request");
		}
		return error(HttpStatus.BAD_GATEWAY, "Downstream service request failed");
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
