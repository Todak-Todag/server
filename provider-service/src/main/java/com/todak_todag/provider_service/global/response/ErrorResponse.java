package com.todak_todag.provider_service.global.response;

import com.todak_todag.provider_service.global.exception.ErrorCode;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        ErrorDetail details,
        Instant timestamp
) {

    public record ErrorDetail(
            String reason
    ) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                message,
                new ErrorDetail(errorCode.getMessage()),
                Instant.now()
        );
    }
}
