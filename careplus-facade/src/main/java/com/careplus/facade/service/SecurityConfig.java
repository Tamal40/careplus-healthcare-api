package com.careplus.facade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {
	
	@Autowired
	JwtAuthenticationFilter jwtFilter;
	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public AuthenticationManager authenticationManager(
	       UserDetailsService service,PasswordEncoder encoder)
	        throws Exception {

	    DaoAuthenticationProvider dao=new 
	    		DaoAuthenticationProvider(service);
	    dao.setPasswordEncoder(encoder);
	    return new ProviderManager(dao);
	}
	
	@Bean
	public SecurityFilterChain chain(HttpSecurity http) {
		http.csrf(c->c.disable())
		.sessionManagement(se->se.sessionCreationPolicy(
				SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth->auth
		 .requestMatchers(
                 "/oauth2/**",
                 "/login/**",
                 "/error"
             ).permitAll()
		 .requestMatchers(
	                "/index.html",
	                "/index.js",
	                "/app.js",
	                "/constants.js",
	                "/favicon.ico",
	                "/css/**",
	                "/js/**",
	                "/api**"
	            ).authenticated()
		.requestMatchers("/api/admin")
		.hasAnyRole("ADMIN")
		.requestMatchers(
                "/api/patients/**"
        )
        .hasAnyRole(
                "PATIENT",
                "DOCTOR",
                "ADMIN"
        )
        .requestMatchers("/api/doctors/**").hasAnyRole("DOCTOR","ADMIN","PATIENT").
        requestMatchers("/api/appointments/**").hasAnyRole("DOCTOR","ADMIN","PATIENT")
        .requestMatchers(
                "/api/medical-records/**"
        )
        .hasAnyRole(
                "PATIENT",
                "DOCTOR",
                "ADMIN"
        )
		.anyRequest()
		.authenticated()).
		oauth2Login(oauth -> oauth
                .defaultSuccessUrl("/", true)
            )
		.addFilterBefore(jwtFilter, 
				UsernamePasswordAuthenticationFilter.class);
		;
		return http.build();
	}
	

}
