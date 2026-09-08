package com.todak_todag.user_service.user.presentation.controller.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.AuthLogoutCommand;
import com.todak_todag.user_service.user.application.result.AuthLoginResult;
import com.todak_todag.user_service.user.application.service.command.AuthCommandService;
import com.todak_todag.user_service.user.presentation.cookie.CookieProvider;
import com.todak_todag.user_service.user.presentation.request.UserLoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController implements AuthApiSpec {
	
	private final String accessTokenCookieName;
	
	private final String refreshTokenCookieName;
	
	private final Duration accessMaxAge;
	
	private final Duration refreshMaxAge;
	
	private final AuthCommandService authCommandService;

	private final CookieProvider cookieProvider;
	
	public AuthApiController(
			@Value("${jwt.access.max-age}") Duration accessMaxAge,
			@Value("${jwt.refresh.max-age}") Duration refreshMaxAge,
			@Value("${authentication.access-token.cookie-name}") String accessTokenCookieName,
			@Value("${authentication.refresh-token.cookie-name}") String refreshTokenCookieName,
			AuthCommandService authCommandService,
			CookieProvider cookieProvider
	) {
		if(accessTokenCookieName == null || accessTokenCookieName.isBlank()) {
			log.error(
					"[User] 설정 값 오류 AccessToken CookieName is null"
			);
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 AccessToken 쿠키 이름 설정 값 누락");
		}
		
		if(refreshTokenCookieName == null || refreshTokenCookieName.isBlank()) {
			log.error(
					"[User] 설정 값 오류 RefreshToken CookieName is null"
			);
						
			throw new IllegalArgumentException("[User] 서버 구동 실패 RefreshToken 쿠키 이름 설정 값 누락");
		}
		
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
		this.accessTokenCookieName = accessTokenCookieName;
		this.refreshTokenCookieName = refreshTokenCookieName;
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
				accessTokenCookieName,
				accessMaxAge,
				result.accessToken(),
				httpServletResponse
		);
		
		// RefreshToken Cookie Set
		cookieProvider.addCookie(
				refreshTokenCookieName,
				refreshMaxAge,
				result.refreshToken(),
				httpServletResponse
		);
		
		return ResponseEntity.noContent().build();
	}



	@Override
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			@AuthenticationPrincipal UserContext user,
			HttpServletResponse httpServletResponse,
			HttpServletRequest httpServletRequest
	) {
		
		String accessTokenFromCookie = cookieProvider.getCookieValue(accessTokenCookieName, httpServletRequest);
		
		authCommandService.logout(new AuthLogoutCommand(user, accessTokenFromCookie));
		
		cookieProvider.addCookie(accessTokenCookieName, Duration.ZERO, "", httpServletResponse);
		cookieProvider.addCookie(refreshTokenCookieName, Duration.ZERO, "", httpServletResponse);
		
		return ResponseEntity.noContent().build();
	}



	@Override
	@PostMapping("/reissue")
	public ResponseEntity<ApiResponse<Void>> reissue(
			HttpServletResponse httpServletResponse,
			HttpServletRequest httpServletRequest
	) {
		
		String refreshToken = cookieProvider.getCookieValue(refreshTokenCookieName, httpServletRequest);
		
		
		
		return null;
	}
	
	
}
