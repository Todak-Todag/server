package com.todak_todag.provider_service.global.response;

import com.todak_todag.provider_service.global.exception.ErrorCode;

import java.time.Instant;

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
