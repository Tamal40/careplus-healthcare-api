package com.careplus.facade.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careplus.facade.dto.LoginRequest;
import com.careplus.facade.dto.LoginResponse;
import com.careplus.facade.service.AuthenticationService;
import com.careplus.facade.service.CookieAuthorizationRequestRepository;
import com.careplus.facade.service.CustomUserDetailsService;
import com.careplus.facade.service.GoogleLoginSuccessHandler;
import com.careplus.facade.service.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
	
	@Autowired
	 private AuthenticationService authService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private CustomUserDetailsService userDetailsService;
	
	@PostMapping("/login")
	 public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest req){
		 String token = authService.login(req);
		 LoginResponse res=new LoginResponse();
		 res.setToken(token);

		 return ResponseEntity.status(HttpStatusCode.valueOf(200)).
					body(res);
		 
	 }

	/**
	 * Trades the one-shot cookie set by the Google callback for this app's JWT, so a known user skips the
	 * username/password screen. No session is involved: the cookie itself carries a signed JWT, so any
	 * instance can answer. 401 means there is no (valid) Google login cookie.
	 */
	@GetMapping("/google-token")
	public ResponseEntity<Map<String, String>> googleToken(HttpServletRequest request, HttpServletResponse response) {
		String token = null;
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (GoogleLoginSuccessHandler.COOKIE_NAME.equals(cookie.getName())) {
					token = cookie.getValue();
				}
			}
		}
		if (token == null || token.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			String username = jwtService.extractUserName(token);
			UserDetails user = userDetailsService.loadUserByUsername(username);
			if (!jwtService.isTokenValid(token, user)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			String role = user.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
			// One-shot: the UI keeps the token itself from here on.
			CookieAuthorizationRequestRepository.addCookie(response, GoogleLoginSuccessHandler.COOKIE_NAME, "", 0,
					request.isSecure());
			return ResponseEntity.ok(Map.of("token", token, "username", username, "role", role));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

}
