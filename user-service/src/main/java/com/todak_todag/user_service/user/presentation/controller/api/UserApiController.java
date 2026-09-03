package com.todak_todag.user_service.user.presentation.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.result.UserResult.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserResult.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.response.UserResponse.UserAdminCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserResponse.UserSignupCreatedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class UserApiController implements UserApiSpec {

	private final UserCreateService userCreateService;
	
	@Override
	@PostMapping("/users/signup")
	public ResponseEntity<ApiResponse<UserSignupCreatedResponse>> createUserSignup(
			@Valid @RequestBody UserSignupRequest userSignupRequest
	) {
		UserSignupCreatedResult result = userCreateService.createUserSignup(userSignupRequest.toCommand());
		
		UserSignupCreatedResponse response = UserSignupCreatedResponse.of(result);
		
		return ResponseEntity
				.status(201)
				.body(ApiResponse.created("회원가입 신청이 완료되었습니다.", response));
	}

	@Override
	@PostMapping("/admin/users")
	public ResponseEntity<ApiResponse<UserAdminCreatedResponse>> createAdmin(
			@Valid @RequestBody UserAdminCreateRequest userAdminCreateRequest
	) {
		UserAdminCreatedResult result = userCreateService.createUserAdmin(userAdminCreateRequest.toCommand());
		
		UserAdminCreatedResponse response = new UserAdminCreatedResponse(
				result.userId(),
				result.name(),
				result.province(),
				result.district()
		);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.created("운영자 등록 완료", response));
	}
	
	
}