package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsentErrorCode implements ErrorCode {

    INVALID_CONSENT_DOCUMENT_VERSION(HttpStatus.NOT_FOUND, "동의할 수 없는 약관 버전이 포함되어 있습니다."),
    CONSENT_ALREADY_AGREED(HttpStatus.CONFLICT, "이미 동의한 약관이 포함되어 있습니다."),
    DUPLICATE_CONSENT_DOCUMENT_VERSION(HttpStatus.BAD_REQUEST, "중복된 약관 버전이 포함되어 있습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}