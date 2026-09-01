package com.todak_todag.user_service.global.exception;

public class UserException extends BusinessException {

	public UserException(UserErrorCode errorCode) {
		super(errorCode);
	}

}
