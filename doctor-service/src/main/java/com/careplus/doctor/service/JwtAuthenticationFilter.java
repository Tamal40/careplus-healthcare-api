package com.careplus.doctor.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	
	@Autowired
	private JwtService service;
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String authHeader=request.getHeader(HttpHeaders.AUTHORIZATION);
		 System.out.println("JWT filter reached");
		    System.out.println("Request URI: " + request.getRequestURI());
		    System.out.println("Authorization present: " + (authHeader != null));
		    System.out.println("Bearer token present: "
		            + (authHeader != null && authHeader.startsWith("Bearer ")));
		if(authHeader==null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
			
		}
		String token=authHeader.substring(7);
		try {
		Claims claim=service.extractClaims(token);
		String username=claim.getSubject();
		String role=claim.get("role", String.class);
		if (!role.startsWith("ROLE_")) {
		    role = "ROLE_" + role;
		}
		List<GrantedAuthority> auth=
				List.of(new SimpleGrantedAuthority(role));
		UsernamePasswordAuthenticationToken autht=
				new UsernamePasswordAuthenticationToken(
						username,null,auth);
		SecurityContextHolder.getContext()
		.setAuthentication(autht);
		System.out.println("Username: " + username);
		System.out.println("Role: " + role);
		System.out.println("Auth: " +
		        SecurityContextHolder.getContext()
		                .getAuthentication());
	}
	catch (JwtException |
            IllegalArgumentException ex) {

       SecurityContextHolder.clearContext();
   }

   filterChain.doFilter(
           request,
           response
   );
	

}
	}

