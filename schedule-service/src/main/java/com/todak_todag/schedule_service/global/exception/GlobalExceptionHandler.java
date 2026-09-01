package com.todak_todag.schedule_service.global.exception;

import com.todak_todag.schedule_service.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[Schedule] 비즈니스 예외 발생 errorCode={}", errorCode.getCode());
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("[Schedule] 요청 값 검증 실패 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[Schedule] 요청 파라미터 타입 오류 name={}", e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Schedule] 처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
