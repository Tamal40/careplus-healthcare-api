package com.careplus.facade.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	@Value("${app.jwt.secret}")
	private String secret;
	
	@Value("${app.jwt.expiration-minutes}")
	private Long expiration;
	
	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor
				(secret.getBytes(StandardCharsets.UTF_8));
	}
	
	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
	
	private boolean isTokenExpired(String token) {
		return extractAllClaims(token)
				.getExpiration()
				.before(new Date());
	}
	
	public boolean isTokenValid(String token,
			UserDetails details) {
		String username=extractUserName(token);
		return username.equals(details.getUsername())
				&& !isTokenExpired(token);
	}

	public String extractUserName(String token) {
		return extractAllClaims(token).getSubject();
	}
	
	public String generateToken(UserDetails details) {
		 String role = details
		            .getAuthorities()
		            .iterator()
		            .next()
		            .getAuthority();
		 System.out.println(role);
		return Jwts.builder()
				.subject(details.getUsername())
			    .issuedAt(new Date())
			    .claim("role", role)
			    .expiration(new Date(
                        System.currentTimeMillis()
                        + (expiration * 60 * 1000)))
			    .signWith(getSigningKey())
			    .compact();
	}
	

}
