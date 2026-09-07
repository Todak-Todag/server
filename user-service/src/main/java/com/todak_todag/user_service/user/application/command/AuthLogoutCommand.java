package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.security.UserContext;

public record AuthLogoutCommand(
		UserContext user,
		String accessToken
) {
	
	public UUID requesterId() {
		return user.getUserId();
	}
	
	public UserRole requesterRole() {
		return user.getRole();
	}

}
