package com.todak_todag.user_service.user.presentation.api_controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserApiController implements UserApiSpec {

	private final UserCreateService userCreateService;
	
	@Override
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Object>> createUserSignup() {
		
		
		
		return null;
	}
}
