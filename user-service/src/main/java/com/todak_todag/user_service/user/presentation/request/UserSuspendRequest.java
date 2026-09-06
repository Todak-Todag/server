package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserSuspendCommand;

public record UserSuspendRequest(
		String suspendReason
) {

	public UserSuspendCommand toCommand(UUID userId, UserContext user) {
		return new UserSuspendCommand(
				userId,
				suspendReason,
				user
		);
	}
}
