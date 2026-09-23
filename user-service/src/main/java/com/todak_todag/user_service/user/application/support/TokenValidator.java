package com.todak_todag.user_service.user.application.support;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.AuthErrorCode;
import com.todak_todag.user_service.global.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenValidator {

	private static final Pattern RANDOM_STRING_PATTERN = Pattern.compile("^[A-Za-z0-9]{32}$");

	public void validateRefreshTokenCookie(String refreshTokenCookie) {
		if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
			log.info("[User] RefreshToken 쿠키 없이 재발급이 시도되었습니다.");

			throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
		}
		
		if (!RANDOM_STRING_PATTERN.matcher(refreshTokenCookie).matches()) {
			log.warn(
					"[User] 형식이 올바르지 않은 RefreshToken 으로 재발급이 시도되었습니다. length={}",
					refreshTokenCookie.length()
			);

			throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
		}
	}

}
