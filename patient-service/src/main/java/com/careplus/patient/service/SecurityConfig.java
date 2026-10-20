package com.careplus.patient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter jwtFilter;
	
	@Bean
	public SecurityFilterChain chain(HttpSecurity http) {
		http.csrf(c->c.disable())
		 .sessionManagement(session ->
         session.sessionCreationPolicy(
             SessionCreationPolicy.STATELESS
         )
     )
		.authorizeHttpRequests(
				req->req.requestMatchers( "/health")
				.permitAll()
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
				 .requestMatchers(
	                        "/api/patients/**"
	                )
	                .hasAnyRole(
	                        "PATIENT",
	                        "DOCTOR",
	                        "ADMIN"
	                )
	                .requestMatchers(
	                        "/api/medical-records/**"
	                )
	                .hasAnyRole(
	                        "PATIENT",
	                        "DOCTOR",
	                        "ADMIN"
	                )

	                .anyRequest()
	                .authenticated())
		 .addFilterBefore(
                 jwtFilter,
                 UsernamePasswordAuthenticationFilter.class
         );

     return http.build();
		
		
	}

}
