package com.todak_todag.api_gateway.token;

public record AccessTokenClaims(
		String userId,
		String role
) {}
