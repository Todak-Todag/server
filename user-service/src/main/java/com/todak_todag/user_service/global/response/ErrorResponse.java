package com.todak_todag.user_service.global.response;

import java.time.Instant;

import com.todak_todag.user_service.global.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        ErrorDetail details,
        Instant timestamp
) {

    public record ErrorDetail(String reason) {}

    public static ErrorResponse of(ErrorCode errorCode) {
    	return of(errorCode, errorCode.getMessage());
    }

		private static ErrorResponse of(ErrorCode errorCode, String message) {
			return new ErrorResponse(
					false,
					errorCode.getCode(),
					message,
					new ErrorDetail(errorCode.getMessage()),
					Instant.now()
			);
		}
}
