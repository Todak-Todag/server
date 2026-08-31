package com.todak_todag.user_service.global.exception;

public class AuthException extends BusinessException {

	public AuthException(AuthErrorCode errorCode) {
		super(errorCode);
	}

}
