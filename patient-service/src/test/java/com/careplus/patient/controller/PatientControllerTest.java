package com.careplus.patient.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.careplus.common.dto.AppointmentDto;
import com.careplus.patient.exception.PatientExceptionHandler;
import com.careplus.patient.exception.PatientNotFoundException;
import com.careplus.patient.service.PatientAccess;
import com.careplus.patient.service.PatientService;

/**
 * Exercises the controller through Spring MVC, so @Valid wiring, the exception handler and the response statuses are
 * covered, not just the DTO constraints on their own. Method security (@PreAuthorize) is out of scope here.
 */
@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

	@Mock
	private PatientService service;

	@Mock
	private PatientAccess access;

	@InjectMocks
	private PatientController controller;

	private LocalValidatorFactoryBean validator;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new PatientExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	@Test
	void unknownPatientIsA404WithAnErrorBody() throws Exception {
		when(service.getPatient(99L)).thenThrow(new PatientNotFoundException(99L));

		mvc.perform(get("/api/patients/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.message", is("Patient 99 not found")));
	}

	@Test
	void anotherPatientsRecordsAreA403AndNothingIsLoaded() throws Exception {
		org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("You can only access your own patient record"))
				.when(access).requireAccess(7L);

		mvc.perform(get("/api/patients/7")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status", is(403)));
		mvc.perform(get("/api/patients/7/appointments")).andExpect(status().isForbidden());
		mvc.perform(get("/api/patients/7/medical-history").header(HttpHeaders.AUTHORIZATION, "Bearer t"))
				.andExpect(status().isForbidden());

		verify(service, never()).getPatient(any());
		verify(service, never()).getAppointments(any());
		verify(service, never()).getMedicalHistory(any());
	}

	@Test
	void allowedReadsCheckAccessForTheRequestedPatient() throws Exception {
		when(service.getAppointments(1L)).thenReturn(List.of(new AppointmentDto()));

		mvc.perform(get("/api/patients/1/appointments")).andExpect(status().isOk());

		verify(access).requireAccess(1L);
	}

	@Test
	void meReturnsTheIdOfThePatientBehindTheLogin() throws Exception {
		com.careplus.common.dto.Patient p = new com.careplus.common.dto.Patient();
		p.setId(5L);
		p.setName("Ada");
		p.setEmail("ada@example.com");
		when(service.getPatientByEmail("ada@example.com")).thenReturn(p);

		mvc.perform(get("/api/patients/me")
						.principal(new org.springframework.security.authentication.TestingAuthenticationToken("ada@example.com", null, "ROLE_PATIENT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(5)))
				.andExpect(jsonPath("$.email", is("ada@example.com")));
	}

	@Test
	void meIs404WhenNoPatientIsLinkedToTheLogin() throws Exception {
		when(service.getPatientByEmail("x@example.com")).thenThrow(new PatientNotFoundException("No patient record is linked to this login"));

		mvc.perform(get("/api/patients/me")
						.principal(new org.springframework.security.authentication.TestingAuthenticationToken("x@example.com", null, "ROLE_PATIENT")))
				.andExpect(status().isNotFound());
	}

	@Test
	void appointmentsReadIsA200() throws Exception {
		when(service.getAppointments(1L)).thenReturn(List.of(new AppointmentDto()));

		mvc.perform(get("/api/patients/1/appointments")).andExpect(status().isOk());
	}

	@Test
	void appointmentsOfUnknownPatientIsA404WithAnErrorBody() throws Exception {
		when(service.getAppointments(99L)).thenThrow(new PatientNotFoundException(99L));

		mvc.perform(get("/api/patients/99/appointments"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.message", is("Patient 99 not found")));
	}

	@Test
	void invalidPatientIsA400WithFieldErrorsAndIsNotSaved() throws Exception {
		mvc.perform(post("/api/patients/create").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\" \",\"email\":\"not-an-email\",\"phone\":\"9000000000\",\"date_of_birth\":\"1990-01-01\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Request validation failed")))
				.andExpect(jsonPath("$.fieldErrors.name", is("name is required")))
				.andExpect(jsonPath("$.fieldErrors.email", is("email must be a valid email address")));

		verify(service, never()).createPatient(any());
	}

	@Test
	void malformedJsonIsA400() throws Exception {
		mvc.perform(post("/api/patients/create").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Malformed request body")));
	}

	@Test
	void medicalRecordWithNullPrescriptionIsA400() throws Exception {
		mvc.perform(post("/api/patients/medical-records").contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer token")
						.content("{\"appointment_id\":7,\"diagnosis\":\"Infection\",\"prescriptions\":[null]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Request validation failed")))
				// Proves the 400 comes from the @NotNull on the list element, not from some other cause.
				.andExpect(jsonPath("$.fieldErrors.*", hasItem("prescription entries must not be null")));

		verify(service, never()).createRecord(any(), any());
	}
}
