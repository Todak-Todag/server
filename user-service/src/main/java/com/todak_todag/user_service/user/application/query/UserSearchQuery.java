package com.todak_todag.user_service.user.application.query;

import java.util.Set;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public record UserSearchQuery(
		Integer page,
		Integer size,
		Set<UserRole> roles,
		UserStatus status
) {

}
