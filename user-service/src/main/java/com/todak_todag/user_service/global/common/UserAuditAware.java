package com.todak_todag.user_service.global.common;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class UserAuditAware implements AuditorAware<UUID> {

	private static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	@Override
	public Optional<UUID> getCurrentAuditor() {
		return null;
	}

}
