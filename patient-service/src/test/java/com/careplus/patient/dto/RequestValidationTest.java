package com.careplus.patient.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.careplus.common.dto.Patient;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** The rules @Valid enforces on the patient API's request bodies. */
class RequestValidationTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		factory.close();
	}

	@Test
	void completePatientIsValid() {
		assertThat(validator.validate(validPatient())).isEmpty();
	}

	@Test
	void patientMissingRequiredFieldsReportsEachOne() {
		Patient patient = new Patient();

		assertThat(invalidFields(validator.validate(patient))).contains("name", "email", "phone", "date_of_birth");
	}

	@Test
	void patientWithMalformedEmailAndFutureBirthDateIsInvalid() {
		Patient patient = validPatient();
		patient.setEmail("not-an-email");
		patient.setDate_of_birth(LocalDate.now().plusDays(1));

		assertThat(invalidFields(validator.validate(patient))).containsExactlyInAnyOrder("email", "date_of_birth");
	}

	@Test
	void medicalRecordWithoutPrescriptionsIsValid() {
		MedicalRecordRequest record = new MedicalRecordRequest();
		record.setAppointment_id(7L);
		record.setDiagnosis("Seasonal allergy");

		assertThat(validator.validate(record)).isEmpty();
	}

	@Test
	void medicalRecordValidatesNestedPrescriptions() {
		MedicalRecordRequest record = new MedicalRecordRequest();
		record.setDiagnosis(" ");
		record.setPrescriptions(List.of(new PrescriptionRequest()));

		assertThat(invalidFields(validator.validate(record)))
				.contains("appointment_id", "diagnosis", "prescriptions[0].medicine_name", "prescriptions[0].dosage");
	}

	private static Patient validPatient() {
		Patient p = new Patient();
		p.setName("Ada Lovelace");
		p.setEmail("ada@example.com");
		p.setPhone("9000000000");
		p.setDate_of_birth(LocalDate.of(1990, 1, 1));
		return p;
	}

	private static <T> Set<String> invalidFields(Set<ConstraintViolation<T>> violations) {
		return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
	}
}
