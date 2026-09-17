package com.todak_todag.discharge_service.global.exception;

import com.todak_todag.discharge_service.global.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    @DisplayName("낙관적 락 충돌 발생 시 409 Conflict를 반환한다")
    void optimisticLockingFailureReturnsConflict() {

        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException(
                        "Discharge",
                        "test-id"
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleOptimisticLockingFailure(
                        exception
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(response.getBody())
                .isNotNull();
    }
}