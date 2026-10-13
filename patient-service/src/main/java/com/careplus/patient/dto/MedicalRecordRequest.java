package com.careplus.patient.dto;

import java.util.List;

public class MedicalRecordRequest {
	
	 private Long appointment_id;
	    private String diagnosis;
	    private String notes;
	    private List<PrescriptionRequest> prescriptions;
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
