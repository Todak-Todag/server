package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;

public record UserSignupCreatedResponse(UUID userId, String name) {
	public static UserSignupCreatedResponse of(UserSignupCreatedResult result) {
		return new UserSignupCreatedResponse(
				result.userId(),
				result.name()
		);
	}
}
