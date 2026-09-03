package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
		UNAUTHORIZED_INTERNAL_REQUEST(HttpStatus.UNAUTHORIZED, "잘못된 접근입니다."),
		SERVICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "서비스를 이용할 수 있는 권한이 없습니다."),
		DUPLICATE_REQUEST(HttpStatus.CONFLICT, "요청이 중복 처리되었습니다. 잠시 후 다시 시도해주세요.")
		;
	
    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}