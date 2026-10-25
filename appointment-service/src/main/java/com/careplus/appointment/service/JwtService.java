package com.careplus.appointment.service;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	@Value("${app.jwt.secret}")
	private String secret;
	
	public SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
	
	public Claims extractClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * A one-minute token for service-to-service calls (role SERVICE). The appointment service uses it to change a
	 * doctor's slot capacity, which no user role may do directly.
	 */
	public String generateServiceToken() {
		return Jwts.builder()
				.subject("appointment-service")
				.claim("role", "SERVICE")
				.issuedAt(new java.util.Date())
				.expiration(new java.util.Date(System.currentTimeMillis() + 60_000L))
				.signWith(getSigningKey())
				.compact();
	}

}
