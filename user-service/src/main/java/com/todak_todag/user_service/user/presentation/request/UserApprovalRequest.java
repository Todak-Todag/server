package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

public record UserApprovalRequest(
		UUID userId,
		Boolean accept,
		String rejectReason
) {

}
