package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public record AuthLoginSnapshotResult(
		UUID userId,
		String passwordHash,
		UserRole role,
		boolean withdrawn,
		boolean patientConsent
) {

}
