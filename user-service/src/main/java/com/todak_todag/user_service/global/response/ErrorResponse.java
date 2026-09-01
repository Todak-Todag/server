package com.todak_todag.user_service.global.response;

import java.time.Instant;

import com.todak_todag.user_service.global.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        ErrorDetail error,
        Instant timestamp
) {

    public record ErrorDetail(
            String message,
            String errorCode
    ) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, new ErrorDetail(errorCode.getMessage(), errorCode.getCode()), Instant.now());
    }
}
