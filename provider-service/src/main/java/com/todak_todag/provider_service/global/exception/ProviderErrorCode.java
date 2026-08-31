package com.todak_todag.provider_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProviderErrorCode implements ErrorCode {

    // 400
    PROVIDE_WORK_INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다."),
    PROVIDE_WORK_INVALID_DAY(HttpStatus.BAD_REQUEST, "제공 요일은 1(월)~7(일) 사이의 값이어야 합니다."),

    // 403
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "요청 권한이 없습니다."),

    // 404
    PROVIDE_SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 서비스 종류입니다."),
    SERVICE_OFFERING_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제공 서비스입니다."),
    PROVIDE_WORK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 제공 가능 일정입니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다."),

    // 409
    SERVICE_OFFERING_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 제공 서비스입니다."),
    SERVICE_OFFERING_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 제공 서비스입니다."),
    PROVIDE_WORK_TIME_OVERLAP(HttpStatus.CONFLICT, "같은 요일에 시간이 겹치는 제공 가능 일정이 이미 존재합니다."),
    PROVIDE_WORK_SCHEDULE_EXISTS(HttpStatus.CONFLICT, "이미 확정된 서비스 일정이 존재해 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
