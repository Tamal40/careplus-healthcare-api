package com.careplus.facade.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Completes the Google sign-in without a session: a registered user gets the app's JWT in a short-lived
 * HttpOnly cookie that the UI trades for the token via /api/auth/google-token; anyone else is sent back
 * to the UI with an error flag.
 */
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

	public static final String COOKIE_NAME = "CAREPLUS_GOOGLE_LOGIN";
	private static final int MAX_AGE_SECONDS = 120;

	// Lazy: SecurityConfig needs this handler, and AuthenticationService needs the AuthenticationManager
	// bean that SecurityConfig defines. Resolving it on first use breaks that cycle.
	@Autowired
	@Lazy
	private AuthenticationService authService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		String email = null;
		Boolean verified = null;
		if (authentication.getPrincipal() instanceof OAuth2User googleUser) {
			email = googleUser.getAttribute("email");
			verified = googleUser.getAttribute("email_verified");
		}
		// An unverified email does not prove the caller owns that address, so it cannot select an account.
		if (email == null || !Boolean.TRUE.equals(verified)) {
			response.sendRedirect("/?googleError=unverified");
			return;
		}
		try {
			String token = authService.loginWithGoogle(email).token();
			CookieAuthorizationRequestRepository.addCookie(response, COOKIE_NAME, token, MAX_AGE_SECONDS,
					request.isSecure());
			response.sendRedirect("/");
		} catch (UsernameNotFoundException ex) {
			response.sendRedirect("/?googleError=not_registered");
		}
	}

}
