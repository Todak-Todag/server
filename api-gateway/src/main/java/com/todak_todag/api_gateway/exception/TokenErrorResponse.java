package com.todak_todag.api_gateway.exception;

public record TokenErrorResponse(
		String code,
		String message
) {

	public static TokenErrorResponse from(TokenErrorCode errorCode) {
		return new TokenErrorResponse(errorCode.getCode(), errorCode.getMessage());
	}
}
