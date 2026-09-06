package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

import com.todak_todag.user_service.user.application.service.result.UserInfoResult;

public record UserInfoResponse(
		String name,
		String province,
		String district,
		String phone,
		UUID regionId,
		String role,
		boolean isAddressActive
) {

	public static UserInfoResponse from(UserInfoResult result) {
		return new UserInfoResponse(
				result.name(),
				result.province(),
				result.district(),
				result.phone(),
				result.regionId(),
				result.role().getKoreaName(),
				result.isAddressActive()
		);
	}
}
