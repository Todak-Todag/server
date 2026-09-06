package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsentDocumentErrorCode implements ErrorCode {

    CONSENT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약관을 찾을 수 없습니다."),
    CONSENT_DOCUMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 사용 종료된 약관입니다."),
    CONSENT_DOCUMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "동일한 유형의 약관이 이미 존재합니다."),
    CONSENT_DOCUMENT_VERSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 약관 버전입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}