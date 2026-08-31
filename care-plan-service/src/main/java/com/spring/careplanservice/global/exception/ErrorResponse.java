package com.spring.careplanservice.global.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {

    public static ErrorResponse from(
            ErrorCode errorCode,
            Map<String, Object> details
    ) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                details == null ? Map.of() : details,
                Instant.now()
        );
    }
}