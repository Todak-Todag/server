package com.todak_todag.user_service.global.common;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.security.UserContext;

@Component
public class SignupAuditAware implements AuditorAware<UUID> {

	private static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	@Override
	public Optional<UUID> getCurrentAuditor() {
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if(authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserContext user)) {
			return Optional.of(SYSTEM_UUID);
		}
		
		return Optional.ofNullable(user.getUserId())
				.or(() -> Optional.of(SYSTEM_UUID));
	}

}
