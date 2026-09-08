package com.todak_todag.user_service.user.application.support;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.AuthErrorCode;
import com.todak_todag.user_service.global.exception.BusinessException;

@Component
public class TokenValidator {

	private static final Pattern RANDOM_STRING_PATTERN = Pattern.compile("^[A-Za-z0-9]{32}$");
	
	public void validateRefreshTokenCookie(String refreshTokenCookie) {
		if (refreshTokenCookie == null
				|| refreshTokenCookie.isBlank()
				|| !RANDOM_STRING_PATTERN.matcher(refreshTokenCookie).matches()
		) {
			
      throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
      
		}
	}
	
}
