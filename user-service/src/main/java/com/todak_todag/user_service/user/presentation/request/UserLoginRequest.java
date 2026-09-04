package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.AuthLoginCommand;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
		@NotBlank(message = "로그인 시 아이디는 필수입니다.")
		String username,
		
		@NotBlank(message = "로그인 시 비밀번호는 필수입니다.")
		String password		
) {
	public AuthLoginCommand toCommand() { return new AuthLoginCommand(username, password); }
}
