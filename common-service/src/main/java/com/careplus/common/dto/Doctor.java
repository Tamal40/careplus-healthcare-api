package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;

@Entity
@Table(name="doctor")
public class Doctor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1546930811994960139L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	@Column(nullable=false)
	private String name;
	@Email
	@Column(nullable=false)
	private String email;
	@Column(nullable=false)
	private String specialization;
	@Column(nullable=false)
	private Boolean active;
	@Column(nullable=false)
	private LocalDate created_at;
	@OneToMany(mappedBy = "doctor")
    private List<DoctorAvialability> availabilities = new ArrayList<>();
	public List<DoctorAvialability> getAvailabilities() {
		return availabilities;
	}
	public void setAvailabilities(List<DoctorAvialability> availabilities) {
		this.availabilities = availabilities;
	}
	 // Not part of the JSON: GET /api/doctors is open to patients and must not expose other patients' appointments.
	 // In-process callers still use getAppointments(); appointment data is served by /api/doctors/{id}/appointments.
	 @JsonIgnore
	 @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
		private List<Appointment> appointments = new ArrayList<>();
		public List<Appointment> getAppointments() {
			return appointments;
		}
		public void setAppointments(List<Appointment> appointments) {
			this.appointments = appointments;
		}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getSpecialization() {
		return specialization;
	}
	public void setSpecialization(String specialization) {
		this.specialization = specialization;
	}
	public Boolean getActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	public LocalDate getCreated_at() {
		return created_at;
	}
	public void setCreated_at(LocalDate created_at) {
		this.created_at = created_at;
	}

}
