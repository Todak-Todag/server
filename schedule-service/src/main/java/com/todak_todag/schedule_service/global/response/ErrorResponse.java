package com.todak_todag.schedule_service.global.response;

import com.todak_todag.schedule_service.global.exception.ErrorCode;

import java.time.OffsetDateTime;

public record ErrorResponse(
        boolean success,
        ErrorDetail error,
        OffsetDateTime timestamp
) {

    public record ErrorDetail(
            String message,
            String errorCode
    ) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, new ErrorDetail(errorCode.getMessage(), errorCode.getCode()), OffsetDateTime.now());
    }
}
