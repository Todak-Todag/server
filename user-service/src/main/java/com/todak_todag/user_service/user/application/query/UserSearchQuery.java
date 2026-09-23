package com.todak_todag.user_service.user.application.query;

import java.util.Set;
import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public record UserSearchQuery(
		Integer page,
		Integer size,
		Set<UserRole> roles,
		UserStatus status,
		UUID regionId // ADMIN 요청자의 담당 지역으로 서비스 계층에서만 채워짐 (클라이언트 입력값 아님)
) {

}
