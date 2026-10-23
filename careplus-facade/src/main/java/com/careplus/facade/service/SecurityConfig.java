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

	@Autowired
	CookieAuthorizationRequestRepository authorizationRequestRepository;

	@Autowired
	NoOpAuthorizedClientRepository authorizedClientRepository;

	@Autowired
	GoogleLoginSuccessHandler googleSuccessHandler;
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
		// No HttpSession, so any instance behind the load balancer can serve any request. The Google
		// authorization request travels in a signed cookie and the finished Google login is handed to the UI
		// as a one-shot JWT cookie (see GoogleLoginSuccessHandler). API authorization comes from the JWT.
		.sessionManagement(se->se.sessionCreationPolicy(
				SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth->auth
		 .requestMatchers(
                 "/oauth2/**",
                 "/login/**",
                 "/error",
                 // The UI shell holds no data; without a session it cannot be gated, the API still needs a JWT.
                 "/",
                 "/index.html",
                 "/index.js",
                 "/app.js",
                 "/constants.js",
                 "/favicon.ico",
                 "/css/**",
                 "/js/**",
                 "/api/auth/login",
                 "/api/auth/google-token",
                 "/logout"
             ).permitAll()
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
	            .authorizationEndpoint(endpoint -> endpoint
	                .authorizationRequestResolver(resolver)
	                .authorizationRequestRepository(authorizationRequestRepository)
	            )
	            .authorizedClientRepository(authorizedClientRepository)
	            .successHandler(googleSuccessHandler)
	        )
		.logout(logout -> logout
	            .logoutUrl("/logout")
	            .invalidateHttpSession(false)
	            .clearAuthentication(true)
	            .deleteCookies(GoogleLoginSuccessHandler.COOKIE_NAME,
	            		CookieAuthorizationRequestRepository.COOKIE_NAME)
	            .logoutSuccessUrl("/oauth2/authorization/google")
	            .permitAll()
	        )	.addFilterBefore(jwtFilter, 
				UsernamePasswordAuthenticationFilter.class);
		;
		return http.build();
	}
	

}

