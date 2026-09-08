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

    MATCHING_ALREADY_IN_PROGRESS(
            "SOCIAL_WORKER_MATCHING_ALREADY_IN_PROGRESS",
            HttpStatus.CONFLICT,
            "이미 진행 중인 매칭 요청이 존재합니다."
    );

    private final String code;
    private final HttpStatus status;
    private final String message;
}