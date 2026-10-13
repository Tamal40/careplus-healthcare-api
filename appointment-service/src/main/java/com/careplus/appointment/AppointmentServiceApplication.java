package com.careplus.appointment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EntityScan(basePackages = {
	    "com.careplus.appointment",
	    "com.careplus.common.dto"
	})
@EnableScheduling
public class AppointmentServiceApplication {
 public static void main(String[] args) { SpringApplication.run(AppointmentServiceApplication.class,args); }
}