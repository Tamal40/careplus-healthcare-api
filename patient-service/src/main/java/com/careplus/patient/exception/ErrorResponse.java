package com.careplus.patient.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Body of every error response from the patient API, so clients can rely on one shape: the HTTP status, a
 * human-readable message and, for validation failures, the message for each invalid field.
 */
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors, Instant timestamp) {

	public static ErrorResponse of(int status, String error, String message) {
		return new ErrorResponse(status, error, message, Map.of(), Instant.now());
	}
}
