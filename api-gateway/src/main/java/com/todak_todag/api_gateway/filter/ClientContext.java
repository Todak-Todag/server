package com.todak_todag.api_gateway.filter;

import java.security.Principal;

public record ClientContext(
		String userId,
		String role
) implements Principal {

	@Override
	public String getName() {
		return userId;
	}
	
}
