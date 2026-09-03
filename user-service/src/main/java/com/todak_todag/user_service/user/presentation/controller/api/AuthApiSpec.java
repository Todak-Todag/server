package com.todak_todag.user_service.user.presentation.controller.api;

import org.springframework.http.ResponseEntity;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserLoginRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Tag(name = "Service Auth", description = "인증 API")
public interface AuthApiSpec {

	@Operation(
			summary = "로그인",
			description = """
					사용자 인증을 진행하고 토큰/쿠키를 발급합니다.
					
					APPROVED 상태인 사용자만 로그인이 가능합니다.
					
					로그인 후 AccessToken 과 RefreshToken 을 쿠키로 발급받습니다.
					
					- AccessToken CookieName = AccessToken
					- RefreshToken CookieName = RefreshToken
					
					204 no-content 응답
			"""
	)
	ResponseEntity<ApiResponse<Void>> login(
			@Parameter(description = "로그인 정보", required = true)
			@Valid
			UserLoginRequest userLoginRequest,
			
			@Parameter(hidden = true)
			HttpServletResponse httpServletResponse
	);
}
