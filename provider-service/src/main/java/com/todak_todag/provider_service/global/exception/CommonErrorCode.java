package com.todak_todag.provider_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    UNAUTHORIZED_INTERNAL_REQUEST(HttpStatus.UNAUTHORIZED, "잘못된 접근입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    // DB 커넥션 풀 고갈처럼 일시적인 과부하 — 재시도하면 성공할 수 있다
    SERVICE_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
