package com.careplus.doctor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
public class SecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter jwtFilter;
	@Bean
	public SecurityFilterChain chain(HttpSecurity http) {
		return http.csrf(c->c.disable())
		.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		  .authorizeHttpRequests(
				  auth->auth.requestMatchers("/api/doctors/**").hasAnyRole("DOCTOR","ADMIN","PATIENT")
				  .requestMatchers(
			                "/",
			                "/index.html",
			                "/index.js",
			                "/app.js",
			                "/constants.js",
			                "/favicon.ico",
			                "/css/**",
			                "/js/**"
			            ).permitAll()
				  .requestMatchers("/actuator/health").permitAll()
				  .anyRequest().authenticated())
		  .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
		  .build();
		  
	}

}
