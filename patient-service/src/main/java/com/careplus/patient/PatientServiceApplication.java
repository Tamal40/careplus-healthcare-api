package com.careplus.patient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = {
	    "com.careplus.patient",
	    "com.careplus.common.dto"
	})
public class PatientServiceApplication {
 public static void main(String[] args) { 
 	SpringApplication.run(PatientServiceApplication.class,args); 
 }
}