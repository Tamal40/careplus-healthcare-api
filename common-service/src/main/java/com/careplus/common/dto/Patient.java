package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

@Entity
@Table(name="patient")
public class Patient implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1990931161228199714L;
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	@NotBlank(message = "name is required")
	@Column(nullable=false)
	private String name;
	@NotBlank(message = "email is required")
	@Email(message = "email must be a valid email address")
	@Column(nullable=false)
	private String email;
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	@NotBlank(message = "phone is required")
	@Column(nullable=false)
	private String phone;
	@NotNull(message = "date_of_birth is required")
	@PastOrPresent(message = "date_of_birth cannot be in the future")
	@Column(nullable=false)
	private LocalDate date_of_birth;
	@Enumerated(EnumType.STRING)
	private Gender gender;
	@Column(nullable=false)
	private LocalDate created_at;
	@Column(nullable=false)
	private LocalDate updated_at;
	@OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
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
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public LocalDate getDate_of_birth() {
		return date_of_birth;
	}
	public void setDate_of_birth(LocalDate date_of_birth) {
		this.date_of_birth = date_of_birth;
	}
	public Gender getGender() {
		return gender;
	}
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	public LocalDate getCreated_at() {
		return created_at;
	}
	public void setCreated_at(LocalDate created_at) {
		this.created_at = created_at;
	}
	public LocalDate getUpdated_at() {
		return updated_at;
	}
	public void setUpdated_at(LocalDate updated_at) {
		this.updated_at = updated_at;
	}


}
