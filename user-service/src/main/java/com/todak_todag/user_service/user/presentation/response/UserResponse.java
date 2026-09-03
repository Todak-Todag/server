package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

import com.todak_todag.user_service.user.application.result.UserResult.UserSignupCreatedResult;

public final class UserResponse {

	public record UserSignupCreatedResponse(UUID userId, String name) {
		public static UserSignupCreatedResponse of(UserSignupCreatedResult result) {
			return new UserSignupCreatedResponse(
					result.userId(),
					result.name()
			);
		}
	}
	
	public record UserAdminCreatedResponse(
			UUID userId,
			String name,
			String province,
			String district
	) {}
}
