package com.careplus.facade.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps the in-flight Google authorization request (state, nonce) in a short-lived signed cookie instead of
 * the HttpSession, so the Google callback can land on any instance behind the load balancer.
 */
@Component
public class CookieAuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	public static final String COOKIE_NAME = "CAREPLUS_OAUTH2_REQUEST";
	private static final int MAX_AGE_SECONDS = 180;

	@Value("${app.jwt.secret}")
	private String secret;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		Cookie cookie = findCookie(request);
		return cookie == null ? null : deserialize(cookie.getValue());
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
			HttpServletRequest request, HttpServletResponse response) {
		if (authorizationRequest == null) {
			clear(request, response);
			return;
		}
		addCookie(response, serialize(authorizationRequest), MAX_AGE_SECONDS, request.isSecure());
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
			HttpServletResponse response) {
		OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
		clear(request, response);
		return authorizationRequest;
	}

	private void clear(HttpServletRequest request, HttpServletResponse response) {
		addCookie(response, "", 0, request.isSecure());
	}

	public static void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds,
			boolean secure) {
		response.addHeader("Set-Cookie", ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.path("/")
				// Lax: the return from Google is a top-level GET navigation, which must carry the cookie.
				.sameSite("Lax")
				.maxAge(maxAgeSeconds)
				.build().toString());
	}

	private void addCookie(HttpServletResponse response, String value, long maxAgeSeconds, boolean secure) {
		addCookie(response, COOKIE_NAME, value, maxAgeSeconds, secure);
	}

	private Cookie findCookie(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (COOKIE_NAME.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(authorizationRequest);
			}
			byte[] payload = bytes.toByteArray();
			Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			return encoder.encodeToString(payload) + "." + encoder.encodeToString(sign(payload));
		} catch (Exception ex) {
			throw new IllegalStateException("Could not store the authorization request", ex);
		}
	}

	private OAuth2AuthorizationRequest deserialize(String value) {
		try {
			String[] parts = value.split("\\.");
			if (parts.length != 2) {
				return null;
			}
			Base64.Decoder decoder = Base64.getUrlDecoder();
			byte[] payload = decoder.decode(parts[0]);
			// Verify the signature before touching the bytes with the deserializer.
			if (!MessageDigest.isEqual(sign(payload), decoder.decode(parts[1]))) {
				return null;
			}
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
				in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
						"org.springframework.security.**;java.util.*;java.lang.*;!*"));
				return (OAuth2AuthorizationRequest) in.readObject();
			}
		} catch (Exception ex) {
			return null;
		}
	}

	private byte[] sign(byte[] payload) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return mac.doFinal(payload);
	}

}
