package com.careplus.facade.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careplus.facade.dto.LoginRequest;
import com.careplus.facade.dto.LoginResponse;
import com.careplus.facade.service.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
	
	@Autowired
	 private AuthenticationService authService;
	
	@PostMapping("/login")
	 public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest req){
		 String token = authService.login(req);
		 LoginResponse res=new LoginResponse();
		 res.setToken(token);

		 return ResponseEntity.status(HttpStatusCode.valueOf(200)).
					body(res);
		 
	 }

	/**
	 * Exchanges the Google login held in the session for this app's JWT, so a known user skips the
	 * username/password screen. 404 means the Google account has no CarePlus user; the UI then falls back to
	 * the sign-in screen. Called without a Bearer header, so the principal here is the Google user.
	 */
	@GetMapping("/google-token")
	public ResponseEntity<Map<String, String>> googleToken(@AuthenticationPrincipal OAuth2User googleUser) {
		if (googleUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String email = googleUser.getAttribute("email");
		Boolean emailVerified = googleUser.getAttribute("email_verified");
		// An unverified email does not prove the caller owns that address, so it cannot select an account.
		if (email == null || !Boolean.TRUE.equals(emailVerified)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			AuthenticationService.GoogleLogin login = authService.loginWithGoogle(email);
			return ResponseEntity.ok(Map.of("token", login.token(), "username", login.username(), "role", login.role()));
		} catch (UsernameNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No CarePlus user for " + email));
		}
	}

}
