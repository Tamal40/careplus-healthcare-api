package com.careplus.patient.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MedicalRecordRequest {
	
	@NotNull(message = "appointment_id is required")
	 private Long appointment_id;
	@NotBlank(message = "diagnosis is required")
	    private String diagnosis;
	    private String notes;
	// @NotNull on the element: @Valid alone skips null entries, so [null] passed validation and then NPE'd.
	    private List<@NotNull(message = "prescription entries must not be null") @Valid PrescriptionRequest> prescriptions;
		public Long getAppointment_id() {
			return appointment_id;
		}
		public void setAppointment_id(Long appointment_id) {
			this.appointment_id = appointment_id;
		}
		public String getDiagnosis() {
			return diagnosis;
		}
		public void setDiagnosis(String diagnosis) {
			this.diagnosis = diagnosis;
		}
		public String getNotes() {
			return notes;
		}
		public void setNotes(String notes) {
			this.notes = notes;
		}
		public List<PrescriptionRequest> getPrescriptions() {
			return prescriptions;
		}
		public void setPrescriptions(List<PrescriptionRequest> prescriptions) {
			this.prescriptions = prescriptions;
		}
	    
	    

}
