package com.todak_todag.api_gateway.token;

public record IssuedToken(
		String token,
		long expiresInSeconds
) {

}
