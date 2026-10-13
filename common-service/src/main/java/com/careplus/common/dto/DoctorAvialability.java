package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name="doctor_availability")
public class DoctorAvialability implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5707322594525600187L;
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name = "doctor_id", nullable = false)
	@JsonIgnore
	private Doctor doctor;
	@Column(nullable=false)
	private LocalDate available_date;
	@Column(nullable=false)
	private LocalTime start_time;
	@Column(nullable=false)
	private LocalTime end_time;
	@Column(nullable=false)
	private Integer max_appoinments;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Doctor getDoctor_id() {
		return doctor;
	}
	public void setDoctor_id(Doctor doctor) {
		this.doctor = doctor;
	}
	public LocalDate getAvailable_date() {
		return available_date;
	}
	public void setAvailable_date(LocalDate available_date) {
		this.available_date = available_date;
	}
	public LocalTime getStart_time() {
		return start_time;
	}
	public void setStart_time(LocalTime start_time) {
		this.start_time = start_time;
	}
	public LocalTime getEnd_time() {
		return end_time;
	}
	public void setEnd_time(LocalTime end_time) {
		this.end_time = end_time;
	}
	public Integer getMax_appoinments() {
		return max_appoinments;
	}
	public void setMax_appoinments(Integer max_appoinments) {
		this.max_appoinments = max_appoinments;
	}
	

}
