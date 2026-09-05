package com.todak_todag.user_service.user.application.service.result;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public record UserInfoResult(
		String name,
		String province,
		String district,
		String phone,
		UUID regionId,
		UserRole role
) {

}
