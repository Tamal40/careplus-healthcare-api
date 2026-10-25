package com.careplus.patient.exception;

/**
 * Thrown when a patient id does not exist. Mapped to 404 by {@link PatientExceptionHandler}, replacing the
 * empty Patient body the controller used to return, which a client could not tell apart from a real record.
 */
public class PatientNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PatientNotFoundException(Long id) {
		super("Patient " + id + " not found");
	}

	public PatientNotFoundException(String message) {
		super(message);
	}
}
