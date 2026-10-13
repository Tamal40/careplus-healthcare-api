package com.careplus.facade.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	private JwtService service;
	
	@Autowired
	private CustomUserDetailsService user;
	@Override
	protected void doFilterInternal(
	        HttpServletRequest request,
	        HttpServletResponse response,
	        FilterChain filterChain)
	        throws ServletException, IOException {

	    System.out.println("1. JWT FILTER START");

	    String authHeader =
	            request.getHeader(HttpHeaders.AUTHORIZATION);

	    System.out.println(
	            "2. HEADER PRESENT = " + (authHeader != null)
	    );

	    if (authHeader == null ||
	            !authHeader.startsWith("Bearer ") && request.getRequestURL().toString()
	            .equals("/api/auth/login")) {

	        System.out.println("3. NO BEARER TOKEN");

	        filterChain.doFilter(request, response);
	        return;
	    }

	    String token = authHeader.substring(7);

	    System.out.println("3. TOKEN EXTRACTED");

	    try {

	        String username =
	                service.extractUserName(token);

	        System.out.println(
	                "4. USERNAME = " + username
	        );

	        Authentication existing =
	                SecurityContextHolder
	                        .getContext()
	                        .getAuthentication();

	        System.out.println(
	                "5. EXISTING AUTH = " + existing
	        );

	        if (username != null &&
	                existing == null) {

	            System.out.println(
	                    "6. LOADING USER"
	            );

	            UserDetails userDetails =
	                    user
	                            .loadUserByUsername(username);

	            System.out.println(
	                    "7. USER LOADED = "
	                    + userDetails.getUsername()
	            );

	            System.out.println(
	                    "8. AUTHORITIES = "
	                    + userDetails.getAuthorities()
	            );

	            boolean valid =
	                    service.isTokenValid(
	                            token,
	                            userDetails
	                    );

	            System.out.println(
	                    "9. TOKEN VALID = " + valid
	            );

	            if (valid) {

	                UsernamePasswordAuthenticationToken auth =
	                        new UsernamePasswordAuthenticationToken(
	                                userDetails,
	                                null,
	                                userDetails.getAuthorities()
	                        );

	                SecurityContextHolder
	                        .getContext()
	                        .setAuthentication(auth);

	                System.out.println(
	                        "10. AUTHENTICATION SET"
	                );

	                System.out.println(
	                        "11. SECURITY CONTEXT = "
	                        + SecurityContextHolder
	                            .getContext()
	                            .getAuthentication()
	                );
	            }
	        }

	    } catch (Exception ex) {

	        System.out.println(
	                "JWT FILTER EXCEPTION:"
	        );

	        ex.printStackTrace();

	        SecurityContextHolder.clearContext();
	    }

	    System.out.println(
	            "12. CONTINUING FILTER CHAIN"
	    );

	    filterChain.doFilter(request, response);
	}

}
