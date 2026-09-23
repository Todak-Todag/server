package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsentErrorCode implements ErrorCode {

    INVALID_CONSENT_DOCUMENT_VERSION(HttpStatus.NOT_FOUND, "동의할 수 없는 약관 버전이 포함되어 있습니다."),
    CONSENT_ALREADY_AGREED(HttpStatus.CONFLICT, "이미 동의한 약관이 포함되어 있습니다."),
    DUPLICATE_CONSENT_DOCUMENT_VERSION(HttpStatus.BAD_REQUEST, "중복된 약관 버전이 포함되어 있습니다."),
    CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "철회할 동의 내역을 찾을 수 없습니다."),
    CONSENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 동의 내역만 철회할 수 있습니다."),
    CONSENT_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "이미 철회된 동의 내역입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}