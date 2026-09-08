package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserDeleteCommand;

import jakarta.validation.constraints.NotBlank;

public record UserDeleteRequest(
		@NotBlank(message = "회원탈퇴를 진행하기 위해서 현재 비밀번호는 필수입니다.")
		String currentPassword
) {

	public UserDeleteCommand toCommand(UserContext user, String accessToken) {
		return new UserDeleteCommand(
				currentPassword,
				accessToken,
				user
		);
	}
}
