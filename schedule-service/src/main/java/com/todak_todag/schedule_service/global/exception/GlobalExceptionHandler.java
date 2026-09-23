package com.todak_todag.schedule_service.global.exception;

import com.todak_todag.schedule_service.global.response.ErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[Schedule] 비즈니스 예외 발생 errorCode={}", errorCode.getCode());
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("[Schedule] 권한 없는 요청 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(CommonErrorCode.AUTH_FORBIDDEN));
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[Schedule] 요청 본문 파싱 실패 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("[Schedule] 필수 요청 파라미터 누락 name={}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("[Schedule] 요청 파라미터 제약 조건 위반 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    // InternalApiErrorDecoder는 HTTP 응답을 받은 경우만 처리
    // 여기로 오는 Feign 예외는 응답을 받지 못했거나 해석하지 못한 경우뿐
    //   - RetryableException(FeignException 하위, status <= 0): Connection refused / Connect·Read Timeout
    //     → HTTP 응답 자체가 없으므로 downstream status로 나눌 수 없고, 재시도 가능한 장애라 503
    //   - DecodeException(status는 2xx): 응답은 왔지만 역직렬화 실패 → 재시도해도 같으므로 502
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        ErrorCode errorCode = (e.status() <= 0)
                ? FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
                : FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID;

        log.error("[Schedule] 연계 서비스 호출 실패 status={} errorCode={}", e.status(), errorCode.getCode(), e);

        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Schedule] 처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
