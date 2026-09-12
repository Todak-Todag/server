package com.todak_todag.provider_service.global.exception;

import com.todak_todag.provider_service.global.response.ErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 애플리케이션 중복 검증을 동시에 통과한 요청은 DB 유니크 인덱스가 막는다
    // 어떤 제약이 걸렸는지에 따라 응답이 달라야 해서 제약 이름으로 분기한다
    private static final Map<String, ErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
            "uq_provide_services_name", ProviderErrorCode.PROVIDE_SERVICE_DUPLICATE,
            "uq_service_offerings_provider_service", ProviderErrorCode.SERVICE_OFFERING_DUPLICATE
    );

    // Hibernate가 감싼 예외에서 제약 이름을 꺼낸다. PostgreSQL은 소문자로 돌려준다
    private String extractConstraintName(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String name = constraintViolation.getConstraintName();

                return (name == null) ? null : name.toLowerCase();
            }

            cause = cause.getCause();
        }

        return null;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[Provider] 비즈니스 예외 발생 errorCode={}", errorCode.getCode());
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("[Provider] 요청 값 검증 실패 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[Provider] 요청 파라미터 타입 오류 name={}", e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("[Provider] 필수 요청 파라미터 누락 name={}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("[Provider] 접근 권한 없음 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ProviderErrorCode.AUTH_FORBIDDEN));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String constraintName = extractConstraintName(e);
        ErrorCode errorCode = CONSTRAINT_ERROR_CODES.get(constraintName);

        // 매핑되지 않은 제약 위반은 예상하지 못한 상황이다
        // 409로 뭉뚱그리면 원인을 숨기게 되므로 500으로 두고 원문을 남긴다
        if (errorCode == null) {
            log.error("[Provider] 매핑되지 않은 데이터 무결성 위반 constraint={}", constraintName, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
        }

        log.warn("[Provider] 데이터 무결성 위반 constraint={}", constraintName);

        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        log.error("[Provider] 외부 서비스 호출 실패 status={}", e.status(), e);
        return ResponseEntity.status(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getStatus())
                .body(ErrorResponse.of(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Provider] 처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

}
