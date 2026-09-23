package com.todak_todag.user_service.user.presentation.request;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPatientCreateRequest(
		@NotBlank
		@Pattern(
		    regexp = "^(?=.*[A-Za-z])(?=.*\\d)[a-z][A-Za-z0-9]{5,}$",
		    message = "아이디는 6자 이상이며, 영문 소문자로 시작하고 영문과 숫자를 포함해야 합니다."
		)
		@Size(
				max = 20,
				message = "로그인 아이디는 최대 20자입니다."
		)
		String username,
		
		@NotBlank
		@Pattern(
		    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,}$",
		    message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
		)
		@Size(
				max = 20,
				message = "비밀번호는 최대 20자입니다."
		)
		String password,
		
		@NotBlank
		@Pattern(
		    regexp = "^[A-Za-z가-힣]+$",
		    message = "이름에는 숫자, 특수문자, 공백을 사용할 수 없습니다."
		)
		String name,
		
		@NotBlank
		@Pattern(
		    regexp = "^\\d{9,11}$",
		    message = "전화번호는 '-' 없이 9~11자리 숫자로 입력해야 합니다."
		)
		@Size(max = 20)
		String phone,
		
		UUID regionId,
		
		String address
) {

	public UserPatientCreateCommand toCommand(UserContext user) {
		return new UserPatientCreateCommand(
				username,
				password,
				name,
				phone,
				regionId,
				address,
				user
		);
	}
}
