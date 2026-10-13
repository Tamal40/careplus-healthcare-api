package com.careplus.common.dto;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription")
public class Prescription implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1049787780778039319L;
	
	  @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(
	        name = "medical_record_id",
	        nullable = false
	    )
	    private MedicalRecord medicalRecord;

	    @Column( nullable = false)
	    private String medicine_name;

	    @Column(nullable = false)
	    private String dosage;

	    @Column(nullable = false)
	    private String frequency;

	    @Column(nullable = false)
	    private String duration;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MedicalRecord getMedicalRecord() {
			return medicalRecord;
		}

		public void setMedicalRecord(MedicalRecord medicalRecord) {
			this.medicalRecord = medicalRecord;
		}

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
