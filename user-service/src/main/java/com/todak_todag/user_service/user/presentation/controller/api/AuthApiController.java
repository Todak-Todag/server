package com.todak_todag.user_service.user.presentation.controller.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.application.result.AuthResult.AuthLoginResult;
import com.todak_todag.user_service.user.application.service.command.AuthCommandService;
import com.todak_todag.user_service.user.presentation.cookie.CookieProvider;
import com.todak_todag.user_service.user.presentation.request.UserLoginRequest;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController implements AuthApiSpec {
	
	private static final String ACCESS_TOKEN_COOKIE_NAME = "AccessToken";
	
	private static final String REFRESH_TOKEN_COOKIE_NAME = "RefreshToken";
	
	private final Duration accessMaxAge;
	
	private final Duration refreshMaxAge;
	
	private final AuthCommandService authCommandService;

	private final CookieProvider cookieProvider;
	
	public AuthApiController(
			@Value("${jwt.access.max-age}") Duration accessMaxAge,
			@Value("${jwt.refresh.max-age}") Duration refreshMaxAge,
			AuthCommandService authCommandService,
			CookieProvider cookieProvider
	) {
		if(accessMaxAge == null || accessMaxAge.isNegative()) {
			log.error(
					"[User] 설정 값 오류 jwt.access.max-age={}/s",
					accessMaxAge.getSeconds()
			);
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 쿠키 max-age 설정 값 오류");
		}
		
		if(refreshMaxAge == null || refreshMaxAge.isNegative()) {
			log.error(
					"[User] 설정 값 오류 jwt.refresh.max-age={}/s",
					refreshMaxAge.getSeconds()
			);
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 쿠키 max-age 설정 값 오류");
		}
		
		if(refreshMaxAge.compareTo(accessMaxAge) < 0) {
			log.error("[User] 쿠키 max-age 설정 오류 refresh.max-age < access.max-age");
			
			throw new IllegalStateException("[User] RefreshToken MaxAge 는 AccessToken MaxAge 보다 길어야 합니다.");
		}
		
		this.accessMaxAge = accessMaxAge;
		this.refreshMaxAge = refreshMaxAge;
		this.authCommandService = authCommandService;
		this.cookieProvider = cookieProvider;
	}



	@Override
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<Void>> login(
			@Valid @RequestBody UserLoginRequest userLoginRequest,
			HttpServletResponse httpServletResponse
	) {
		
		AuthLoginResult result = authCommandService.login(userLoginRequest.toCommand());
		
		// AccessToken Cookie Set
		cookieProvider.addCookie(
				ACCESS_TOKEN_COOKIE_NAME,
				accessMaxAge,
				result.accessToken(),
				httpServletResponse
		);
		
		// RefreshToken Cookie Set
		cookieProvider.addCookie(
				REFRESH_TOKEN_COOKIE_NAME,
				refreshMaxAge,
				result.refreshToken(),
				httpServletResponse
		);
		
		return ResponseEntity.noContent().build();
	}
	
	
}
