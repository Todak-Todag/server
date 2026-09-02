package com.todak_todag.api_gateway.exception;

import java.time.Instant;

public record TokenErrorResponse(
		boolean success,
		ErrorDetail error,
		Instant timestamp
) {

	public record ErrorDetail(
			String message,
			String errorCode
	) {}
	
	public static TokenErrorResponse from(TokenErrorCode errorCode) {
		return new TokenErrorResponse(
				false,
				new ErrorDetail(errorCode.getMessage(), errorCode.getCode()),
				Instant.now()
		);
	}
	
}
