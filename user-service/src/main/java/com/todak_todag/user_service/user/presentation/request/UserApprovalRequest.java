package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;

import jakarta.validation.constraints.NotNull;

public record UserApprovalRequest(
		
		@NotNull
		Boolean accept,
		
		
		String rejectReason
) {
	public UserApprovalCommand toCommand(UUID userId, UserContext user) {
		return new UserApprovalCommand(
				userId,
				accept,
				rejectReason,
				user
		);
	}
}
