package com.careplus.facade.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="services")
public class ServiceProperties {
	
	 private ServiceConfig patient;
	 private ServiceConfig doctor;
	 private ServiceConfig appointment;

	    public ServiceConfig getPatient() {
		return patient;
	}



	 public void setPatient(ServiceConfig patient) {
		 this.patient = patient;
	 }



	 public ServiceConfig getDoctor() {
		 return doctor;
	 }



	 public void setDoctor(ServiceConfig doctor) {
		 this.doctor = doctor;
	 }



	 public ServiceConfig getAppointment() {
		 return appointment;
	 }



	 public void setAppointment(ServiceConfig appointment) {
		 this.appointment = appointment;
	 }



		public static class ServiceConfig {
	        private String url;

			public String getUrl() {
				return url;
			}

			public void setUrl(String url) {
				this.url = url;
			}
	    }

}
