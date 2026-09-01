package com.todak_todag.api_gateway.exception;

public class TokenException extends RuntimeException {
	
	private final TokenErrorCode errorCode;
	
	public TokenException(TokenErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
	
	public TokenException(TokenErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}
	
}
