package com.spring.careplanservice.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED_INTERNAL_REQUEST(
            HttpStatus.UNAUTHORIZED,
            "내부 API 인증에 실패했습니다."
    ),

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

    COMMON_INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
    ),

    CARE_PLAN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Care Plan 조회 실패"
    ),
    CARE_PLAN_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 Care Plan이 존재합니다."
    ),

    CARE_PLAN_PATIENT_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "퇴원 건의 환자와 Care Plan 대상 환자가 일치하지 않습니다."
    ),

    DISCHARGE_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "실제 퇴원이 완료되지 않았습니다."
    ),

    CARE_PLAN_SERVICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Care Plan 서비스를 찾을 수 없습니다."
    ),

    SERVICE_PREFERENCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "존재하지 않는 서비스 희망 일정입니다."
    ),

    SERVICE_PREFERENCE_DATE_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "희망 날짜는 Care Plan 제공 기간 내에서만 선택할 수 있습니다."
    ),

    SERVICE_PREFERENCE_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "현재 Care Plan 상태에서는 희망 일정을 등록할 수 없습니다."
    ),

    CARE_PLAN_SERVICE_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 선택한 Care Plan 서비스입니다."
    ),

    CARE_PLAN_BAD_REQUEST(
            HttpStatus.CONFLICT,
            "page는 0 이상이어야 합니다."
    ),

    CARE_PLAN_INVALID_STATUS_TRANSITION(
            HttpStatus.BAD_REQUEST,
            "허용되지 않는 Care Plan 상태 전이입니다."
    ),

    CARE_PLAN_DELETE_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "UNDER_REVIEW 상태의 Care Plan만 삭제할 수 있습니다."
    ),

    CARE_PLAN_SERVICE_CANCEL_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "UNDER_REVIEW 상태의 Care Plan만 서비스 항목을 취소할 수 있습니다."
    ),

    CARE_PLAN_SERVICE_ALREADY_DELETED(
            HttpStatus.CONFLICT,
            "이미 취소된 서비스 항목입니다."
    ),

    SERVICE_PREFERENCE_ALREADY_DELETED(
            HttpStatus.CONFLICT,
            "이미 삭제된 희망 일정입니다."
    ),

    SERVICE_PREFERENCE_DELETE_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "현재 Care Plan 상태에서는 희망 일정을 삭제할 수 없습니다."
    )
    ;


    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}