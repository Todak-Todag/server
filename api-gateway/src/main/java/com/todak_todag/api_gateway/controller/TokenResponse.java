package com.todak_todag.api_gateway.controller;

public record TokenResponse(
		String token,
		long expiresInSeconds
) {

}
