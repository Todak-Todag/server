package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public record UserAdminCreatedResult(
		UUID userId,
		String name,
		String province,
		String district
) {}
