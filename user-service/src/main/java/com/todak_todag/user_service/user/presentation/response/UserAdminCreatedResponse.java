package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

public record UserAdminCreatedResponse(
		UUID userId,
		String name,
		String province,
		String district
) {}
