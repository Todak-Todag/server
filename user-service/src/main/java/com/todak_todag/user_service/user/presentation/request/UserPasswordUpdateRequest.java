package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserPasswordUpdateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
		
		@NotBlank(message = "기존 비밀번호는 필수 입력입니다.")
		String currentPassword,
		
		@NotBlank
		@Pattern(
		    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,}$",
		    message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
		)
		@Size(
				max = 20,
				message = "비밀번호는 최대 20자입니다."
		)
		String newPassword
) {
	public UserPasswordUpdateCommand toCommand(String accessToken, UserContext user) {
		return new UserPasswordUpdateCommand(
				currentPassword,
				newPassword,
				accessToken,
				user
		);
	}
}
