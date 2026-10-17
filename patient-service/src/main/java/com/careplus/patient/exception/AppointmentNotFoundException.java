package com.careplus.patient.exception;

/**
 * Thrown when a medical record refers to an appointment that appointment-service does not have. Mapped to 404 by
 * {@link PatientExceptionHandler}; it used to be a bare RuntimeException that reached the client as a 500.
 */
public class AppointmentNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AppointmentNotFoundException(Long appointmentId) {
		super("Appointment " + appointmentId + " not found");
	}
}
