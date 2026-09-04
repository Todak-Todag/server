package com.todak_todag.schedule_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements ErrorCode {

    // 400
    SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY(HttpStatus.BAD_REQUEST, "서비스 종료 일시 이후에만 상태를 변경할 수 있습니다."),
    SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING(HttpStatus.BAD_REQUEST, "SCHEDULED 상태의 서비스 일정만 연기할 수 있습니다."),
    SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED(HttpStatus.BAD_REQUEST, "일정 시작 24시간 전까지만 변경할 수 있습니다."),
    SERVICE_SCHEDULE_INVALID_RESCHEDULE_DATE(HttpStatus.BAD_REQUEST, "일정은 기존 날짜 기준 하루 앞당기거나 하루 미룰 수만 있습니다."),
    SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "당일 일정으로는 변경할 수 없습니다."),
    SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE(HttpStatus.BAD_REQUEST, "Care Plan의 일정 범위를 초과하여 변경할 수 없습니다."),
    SERVICE_RESULTS_TIME_RANGE_INVALID(HttpStatus.BAD_REQUEST, "시작 일시는 종료 일시보다 이전이어야 합니다."),
    SERVICE_SCHEDULE_INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 필터 값입니다."),

    // 404
    SERVICE_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 서비스 일정입니다."),
    SERVICE_RESULTS_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 서비스 수행 결과입니다."),

    // 409
    SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL(HttpStatus.CONFLICT, "취소할 수 없는 상태의 서비스 일정입니다."),
    SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED(HttpStatus.CONFLICT, "완료할 수 없는 상태의 서비스 일정입니다."),
    SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED(HttpStatus.CONFLICT, "일정 시작 24시간 전까지만 취소할 수 있습니다."),
    SERVICE_RESULTS_INVALID_SCHEDULE_STATUS(HttpStatus.CONFLICT, "COMPLETED 또는 NO_SHOW 상태의 서비스 일정만 결과를 등록할 수 있습니다."),
    SERVICE_RESULTS_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 결과가 등록된 서비스 일정입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
