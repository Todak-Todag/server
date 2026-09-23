package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public record UserSearchResult(
		UUID userId,
		String name,
		String phone,
		String province,
		String district,
		UUID regionId,
		UserStatus status,
		UserRole role,
		boolean isDeleted
) {

}
