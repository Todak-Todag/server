package com.todak_todag.discharge_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    COMMON_INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "잘못된 요청입니다."
    ),

    COMMON_INVALID_INPUT_VALUE(
            HttpStatus.BAD_REQUEST,
            "입력값이 올바르지 않습니다."
    ),

    COMMON_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "요청한 리소스를 찾을 수 없습니다."
    ),

    COMMON_UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 미디어 타입입니다."
    ),

    AUTH_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "접근 권한이 없습니다."
    ),

    DISCHARGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "퇴원건을 찾을 수 없습니다."
    ),

    DISCHARGE_INVALID_SCHEDULED_DATE(
            HttpStatus.BAD_REQUEST,
            "퇴원 예정일이 올바르지 않습니다."
    ),

    COMMON_INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}