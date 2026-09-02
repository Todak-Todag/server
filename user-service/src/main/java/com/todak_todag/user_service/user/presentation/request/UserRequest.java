package com.todak_todag.user_service.user.presentation.request;

import java.util.List;
import java.util.UUID;

import com.todak_todag.user_service.user.application.command.UserCommand.UserSignupCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class UserRequest {

	// === 회원가입 요청 바디 === //
	public record UserSignupRequest(
			@NotBlank
			String type,
			
			@NotBlank
			@Pattern(
			    regexp = "^(?=.*[A-Za-z])(?=.*\\d)[a-z][A-Za-z0-9]{5,}$",
			    message = "아이디는 6자 이상이며, 영문 소문자로 시작하고 영문과 숫자를 포함해야 합니다."
			)
			String username,
			
			@NotBlank
			@Pattern(
			    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,}$",
			    message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
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
			String phone,
			
			UUID regionId,
			
			@NotEmpty
			@Valid
			List<Agreement> agreements
	) {
		record Agreement(
				
				@NotNull
				UUID termsId,
								
				@NotNull
				Boolean agreed
		) {}
		
		public UserSignupCommand toCommand() {
			return null;
		}
	}
	
	// === 어드민 등록 요청 바디 === //
	public record UserAdminCreateRequest() {}
}
