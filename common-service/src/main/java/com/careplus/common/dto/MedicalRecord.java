package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "medical_record")
public class MedicalRecord implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4994880574881075810L;
	 public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public Doctor getDoctor() {
		return doctor;
	}

	public void setDoctor(Doctor doctor) {
		this.doctor = doctor;
	}

	public Appointment getAppointment() {
		return appointment;
	}

	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
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

	public List<Prescription> getPrescriptions() {
		return prescriptions;
	}

	public void setPrescriptions(List<Prescription> prescriptions) {
		this.prescriptions = prescriptions;
	}

	public LocalDateTime getCreated_at() {
		return created_at;
	}

	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}

	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    // Many medical records can belong to one patient
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(
	        name = "patient_id",
	        nullable = false
	    )
	    private Patient patient;

	    // Many medical records can be created by one doctor
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(
	        name = "doctor_id",
	        nullable = false
	    )
	    private Doctor doctor;

	    // One appointment should produce at most one medical record
	    @OneToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(
	        name = "appointment_id",
	        nullable = false,
	        unique = true
	    )
	    private Appointment appointment;

	    @Column(nullable = false)
	    private String diagnosis;

	    @Column(columnDefinition = "TEXT")
	    private String notes;

	    // One medical record can contain multiple prescriptions
	    @OneToMany(
	        mappedBy = "medicalRecord",
	        cascade = CascadeType.ALL,
	        orphanRemoval = true
	    )
	    private List<Prescription> prescriptions = new ArrayList<>();

	    @Column(
	        nullable = false,
	        updatable = false
	    )
	    private LocalDateTime created_at;
	

}
