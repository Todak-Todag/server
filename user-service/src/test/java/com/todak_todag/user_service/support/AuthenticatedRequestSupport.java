package com.todak_todag.user_service.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.security.UserContext;

public class AuthenticatedRequestSupport {

	private AuthenticatedRequestSupport() {}
	
	public static RequestPostProcessor asUser(UUID userId, UserRole role) {
		UserContext user = UserContext.from(userId.toString(), role.name());
		
		return authentication(new UsernamePasswordAuthenticationToken(
				user,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
		));
	}
}
