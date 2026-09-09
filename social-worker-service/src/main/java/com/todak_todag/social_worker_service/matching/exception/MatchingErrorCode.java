package com.todak_todag.social_worker_service.matching.exception;

import com.todak_todag.social_worker_service.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements ErrorCode {

    MATCHING_FORBIDDEN(
            "SOCIAL_WORKER_MATCHING_FORBIDDEN",
            HttpStatus.FORBIDDEN,
            "사회복지사 매칭 요청 권한이 없습니다."
    ),

    MATCHING_QUERY_FORBIDDEN(
            "SOCIAL_WORKER_MATCHING_QUERY_FORBIDDEN",
            HttpStatus.FORBIDDEN,
            "사회복지사 매칭 결과 조회 권한이 없습니다."
    ),

    MATCHING_RESULT_NOT_FOUND(
            "MATCHING_RESULT_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "매칭 결과를 찾을 수 없습니다."
    ),

    MATCHING_ALREADY_IN_PROGRESS(
            "SOCIAL_WORKER_MATCHING_ALREADY_IN_PROGRESS",
            HttpStatus.CONFLICT,
            "이미 진행 중인 매칭 요청이 존재합니다."
    ),

    MATCHING_NOT_FOUND(
            "SOCIAL_WORKER_MATCHING_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "존재하지 않는 사회복지사 매칭 결과입니다."
    ),

    INVALID_MATCHING_STATUS(
            "INVALID_MATCHING_STATUS",
            HttpStatus.BAD_REQUEST,
            "ACTIVE 상태에서만 ENDED로 변경할 수 있습니다."
    ),

    MATCHING_STATUS_CHANGE_FORBIDDEN(
            "SOCIAL_WORKER_MATCHING_STATUS_CHANGE_FORBIDDEN",
            HttpStatus.FORBIDDEN,
            "사회복지사 매칭 상태 변경 권한이 없습니다."
    );

    private final String code;
    private final HttpStatus status;
    private final String message;
}