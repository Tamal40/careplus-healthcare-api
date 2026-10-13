package com.careplus.doctor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
@SpringBootApplication
@EntityScan(basePackages = {
	    "com.careplus.doctor",
	    "com.careplus.common.dto"
	})
public class DoctorServiceApplication {
 public static void main(String[] args) { SpringApplication.run(DoctorServiceApplication.class,args); }
}