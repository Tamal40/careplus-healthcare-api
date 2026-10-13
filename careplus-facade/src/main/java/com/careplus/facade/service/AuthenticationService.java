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

	/** The result of a Google sign-in: the app's JWT plus the DB identity it was issued for. */
	public record GoogleLogin(String token, String username, String role) {
	}

	/**
	 * Signs in a user whose identity Google has already verified. Google stands in for the password check
	 * only: the account must exist in the users table (username = Google email), and the JWT carries that
	 * row's username and role, exactly as a username/password login would.
	 *
	 * @throws org.springframework.security.core.userdetails.UsernameNotFoundException when no such user exists
	 */
	public GoogleLogin loginWithGoogle(String email) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(email);
		String role = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
		return new GoogleLogin(jwtService.generateToken(userDetails), userDetails.getUsername(), role);
	}

}
