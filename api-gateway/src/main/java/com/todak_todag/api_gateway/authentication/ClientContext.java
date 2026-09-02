package com.todak_todag.api_gateway.authentication;

import java.security.Principal;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

public record ClientContext(
		String userId,
		String role
) implements Principal {
	
	public ClientContext {
		validate(userId);
		validate(role);
	}
	
	private static void validate(String value) {
		if (value == null || value.isBlank()) {
			throw new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN);
		}
	}

	@Override
	public String getName() {
		return this.userId;
	}
	
}
