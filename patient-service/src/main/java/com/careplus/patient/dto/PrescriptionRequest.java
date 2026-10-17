package com.careplus.patient.dto;

import jakarta.validation.constraints.NotBlank;

public class PrescriptionRequest {

	@NotBlank(message = "medicine_name is required")
	 private String medicine_name;
	@NotBlank(message = "dosage is required")
	    private String dosage;
	    private String frequency;
	    private String duration;
		public String getMedicine_name() {
			return medicine_name;
		}
		public void setMedicine_name(String medicine_name) {
			this.medicine_name = medicine_name;
		}
		public String getDosage() {
			return dosage;
		}
		public void setDosage(String dosage) {
			this.dosage = dosage;
		}
		public String getFrequency() {
			return frequency;
		}
		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}
		public String getDuration() {
			return duration;
		}
		public void setDuration(String duration) {
			this.duration = duration;
		}
	    
}
