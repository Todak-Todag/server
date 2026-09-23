package com.todak_todag.schedule_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// Internal API(Feign) 호출 실패 전용 코드
@Getter
@RequiredArgsConstructor
public enum FeignErrorCode implements ErrorCode {

    // 500
    EXTERNAL_SERVICE_CALL_REJECTED(HttpStatus.INTERNAL_SERVER_ERROR, "연계 서비스 호출이 거부되었습니다."),

    // 502
    EXTERNAL_SERVICE_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "연계 서비스의 응답을 처리할 수 없습니다."),

    // 503
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
