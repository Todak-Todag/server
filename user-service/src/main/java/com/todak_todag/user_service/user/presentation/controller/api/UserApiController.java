<<<<<<< HEAD
package com.todak_todag.user_service.user.presentation.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.result.UserResult.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.response.UserResponse.UserSignupCreatedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserApiController implements UserApiSpec {

	private final UserCreateService userCreateService;
	
	@Override
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Object>> createUserSignup(
			@Valid @RequestBody UserSignupRequest userSignupRequest
	) {
		UserSignupCreatedResult result = userCreateService.createUserSignup(userSignupRequest.toCommand());
		
		UserSignupCreatedResponse response = UserSignupCreatedResponse.of(result);
		
		return ResponseEntity.
				status(201)
				.body(ApiResponse.created("회원가입 신청이 완료되었습니다.", response));
	}
=======
package com.todak_todag.user_service.user.presentation.api_controller;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserApiController {

>>>>>>> 84ad9c4e121592e3b7123bfd12a8915fa3dfd710
}
