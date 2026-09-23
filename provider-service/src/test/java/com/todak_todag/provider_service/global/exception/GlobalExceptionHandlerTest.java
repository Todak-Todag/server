package com.todak_todag.provider_service.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("제약 이름을 꺼내지 못하면 NPE 없이 500")
    void dataIntegrityViolation_noConstraintName() {
        var response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("원인 없음"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}