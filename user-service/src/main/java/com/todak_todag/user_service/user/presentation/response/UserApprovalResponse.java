package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

public record UserApprovalResponse(
		UUID userId,
		String role,
		String rejectReason
) {

}
