package com.careplus.doctor.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class DoctorAvailable {

	private Long id;
	private LocalDate available_date;

	private LocalTime start_time;

	private LocalTime end_time;

	private Integer max_appoinments;
	
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
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
}
