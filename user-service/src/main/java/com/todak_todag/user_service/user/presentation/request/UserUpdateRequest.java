package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserUpdateCommand;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		
		@Pattern(
		    regexp = "^[A-Za-z가-힣]+$",
		    message = "이름에는 숫자, 특수문자, 공백을 사용할 수 없습니다."
		)
		String name,
		
		@Pattern(
		    regexp = "^\\d{9,11}$",
		    message = "전화번호는 '-' 없이 9~11자리 숫자로 입력해야 합니다."
		)
		@Size(max = 20)
		String phone,
		
		UUID regionId,
		
		String address
) {

	public UserUpdateCommand toCommand(UserContext user) {
		return new UserUpdateCommand(
				name,
				phone,
				regionId,
				address,
				user
		);
	}
}
