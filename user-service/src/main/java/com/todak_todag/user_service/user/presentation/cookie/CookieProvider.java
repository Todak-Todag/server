package com.todak_todag.user_service.user.presentation.cookie;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

@Component
public class CookieProvider {
	
	private static final String SAME_SITE = "strict";
	
	private final boolean httpOnly = true;
	
	private final boolean secure;
	
	public CookieProvider(
			@Value("${jwt.secure}") boolean secure
	) {
		this.secure = secure;
	}
	
	public void addCookie(String cookieName, Duration maxAge, String value, HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, createCookie(cookieName, maxAge, value));
	}
	
	private String createCookie(String cookieName, Duration maxAge, String value) {
		ResponseCookie responseCookie = ResponseCookie.from(cookieName, value)
				.path("/")
				.httpOnly(httpOnly)
				.secure(secure)
				.sameSite(SAME_SITE)
				.maxAge(maxAge)
				.build();
		
		return responseCookie.toString();		
	}
}
