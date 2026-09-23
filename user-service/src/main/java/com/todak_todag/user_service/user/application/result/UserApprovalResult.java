package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public record UserApprovalResult(
		UUID userId,
		UserRole role,
		String rejectReason,
		boolean isAccept
) {

}
