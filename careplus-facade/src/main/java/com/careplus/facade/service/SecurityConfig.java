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
	
	@Autowired
	CustomAuthRequestResolver resolver;
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
		// The Google login is the gate to the UI and must be remembered between requests, so it lives in the
		// session. STATELESS dropped it after the callback and sent the browser back to Google in a loop.
		// API authorization still comes from the JWT that the sign-in screen gets from /api/auth/login.
		.sessionManagement(se->se.sessionCreationPolicy(
				SessionCreationPolicy.IF_REQUIRED))
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
	            .authorizationEndpoint(endpoint ->
	                endpoint.authorizationRequestResolver(resolver)
	            )
	            .defaultSuccessUrl("/", true)
	        )
		.logout(logout -> logout
	            .logoutUrl("/logout")
	            .invalidateHttpSession(true)
	            .clearAuthentication(true)
	            .deleteCookies("JSESSIONID")
	            .logoutSuccessUrl("/oauth2/authorization/google")
	            .permitAll()
	        )	.addFilterBefore(jwtFilter, 
				UsernamePasswordAuthenticationFilter.class);
		;
		return http.build();
	}
	

}

