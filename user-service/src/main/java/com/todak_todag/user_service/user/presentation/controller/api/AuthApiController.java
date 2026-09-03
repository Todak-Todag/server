package com.todak_todag.user_service.user.presentation.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.service.command.AuthCommandService;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserLoginRequest;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController implements AuthApiSpec {

	
	
	private final AuthCommandService authCommandService;

	@Override
	public ResponseEntity<ApiResponse<Void>> login(
			@Valid @RequestBody UserLoginRequest userLoginRequest,
			HttpServletResponse httpServletResponse
	) {
		
		
		
		return null;
	}
	
	
}
