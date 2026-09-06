package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsentDocumentErrorCode implements ErrorCode {

    CONSENT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약관을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}