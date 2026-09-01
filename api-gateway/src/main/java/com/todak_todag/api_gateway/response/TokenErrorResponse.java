package com.todak_todag.api_gateway.response;

import com.todak_todag.api_gateway.exception.TokenErrorCode;

public record TokenErrorResponse(
		String code,
		String message
) {

	public static TokenErrorResponse from(TokenErrorCode errorCode) {
		return new TokenErrorResponse(errorCode.getCode(), errorCode.getMessage());
	}
}
