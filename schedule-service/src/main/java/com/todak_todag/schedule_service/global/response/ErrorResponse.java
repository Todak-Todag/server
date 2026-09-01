package com.todak_todag.schedule_service.global.response;

import com.todak_todag.schedule_service.global.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                details == null ? Map.of() : details,
                Instant.now()
        );
    }
}
