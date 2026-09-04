package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;

public record UserApprovalRequest(
		UUID userId,
		Boolean accept,
		String rejectReason
) {
	public UserApprovalCommand toCommand(UserContext user) {
		return new UserApprovalCommand(
				userId,
				accept,
				rejectReason,
				user
		);
	}
}
