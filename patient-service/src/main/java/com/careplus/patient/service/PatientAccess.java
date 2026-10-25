package com.careplus.patient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.careplus.patient.repository.PatientRepository;

/**
 * Row-level rule for patient data: DOCTOR and ADMIN may read any patient, a PATIENT only their own record.
 * "Own" means the patient's email equals the login name in the JWT (for Google sign-in that is the Google email).
 * An unknown patient id is refused exactly like someone else's id, so a patient cannot probe which ids exist.
 */
@Component
public class PatientAccess {

	@Autowired
	private PatientRepository repo;

	/** @throws AccessDeniedException when the caller may not read the patient with this id */
	public void requireAccess(Long patientId) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new AccessDeniedException("Authentication required");
		}
		if (hasRole(auth, "ROLE_DOCTOR") || hasRole(auth, "ROLE_ADMIN")) {
			return;
		}
		if (hasRole(auth, "ROLE_PATIENT") && patientId != null) {
			boolean own = repo.findById(patientId)
					.map(p -> p.getEmail() != null && p.getEmail().equalsIgnoreCase(auth.getName()))
					.orElse(false);
			if (own) {
				return;
			}
		}
		throw new AccessDeniedException("You can only access your own patient record");
	}

	private static boolean hasRole(Authentication auth, String role) {
		return auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
	}
}
