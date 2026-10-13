package com.careplus.facade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.careplus.facade.dto.LoginRequest;

@Service
public class AuthenticationService {
	
	@Autowired
	private AuthenticationManager manager;
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private  CustomUserDetailsService userDetailsService;
	
	public String login(LoginRequest req) {
				manager.authenticate(
						new UsernamePasswordAuthenticationToken(
								req.getUsername(),req.getPassword()));
				 UserDetails userDetails =
			                userDetailsService.loadUserByUsername(
			                        req.getUsername()
			                );

			        return jwtService.generateToken(userDetails);
	}

}
