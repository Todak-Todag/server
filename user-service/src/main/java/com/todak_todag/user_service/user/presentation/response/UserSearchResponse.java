package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public record UserSearchResponse(
		UUID userId,
		String name,
		String phone,
		String province,
		String district,
		UUID regionId,
		UserStatus status,
		String role,
		boolean isDeleted
) {

	public static UserSearchResponse from(UserSearchResult result) {
		return new UserSearchResponse(
				result.userId(),
				result.name(),
				result.phone(),
				result.province(),
				result.district(),
				result.regionId(),
				result.status(),
				result.role().getKoreaName(),
				result.isDeleted()
		);
	}
}
