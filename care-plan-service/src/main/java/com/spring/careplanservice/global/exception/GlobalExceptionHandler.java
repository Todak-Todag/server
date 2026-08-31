package com.spring.careplanservice.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        Map<String, Object> details = e.getDetails() == null || e.getDetails().isEmpty()
                ? Map.of("reason", e.getMessage())
                : e.getDetails();

        log.warn(
                "[BusinessException] code={}, message={}",
                errorCode.getCode(),
                e.getMessage()
        );

        return createResponse(
                errorCode,
                details
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException e
    ) {
        log.warn(
                "[IllegalStateException] message={}",
                e.getMessage()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of("reason", e.getMessage())
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(
            NoSuchElementException e
    ) {
        log.warn(
                "[NoSuchElementException] message={}",
                e.getMessage()
        );

        return createResponse(
                ErrorCode.COMMON_NOT_FOUND,
                Map.of("reason", e.getMessage())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn(
                "[IllegalArgumentException] message={}",
                e.getMessage()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of("reason", e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn(
                "[MethodArgumentTypeMismatchException] name={}, value={}",
                e.getName(),
                e.getValue()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of(
                        "reason",
                        "%s의 형식이 올바르지 않습니다.".formatted(e.getName())
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        Map<String, Object> details = extractFieldErrors(
                e.getBindingResult().getFieldErrors()
        );

        log.warn(
                "[MethodArgumentNotValidException] errors={}",
                details
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                details
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e
    ) {
        Map<String, Object> details = extractFieldErrors(
                e.getBindingResult().getFieldErrors()
        );

        log.warn(
                "[BindException] errors={}",
                details
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                details
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.warn(
                "[HttpMessageNotReadableException] message={}",
                e.getMessage()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of("reason", "요청 본문의 형식이 올바르지 않습니다.")
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        log.warn(
                "[MissingServletRequestParameterException] parameter={}",
                e.getParameterName()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                Map.of(
                        "reason",
                        "%s 파라미터는 필수입니다.".formatted(e.getParameterName())
                )
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e
    ) {
        log.warn(
                "[MissingServletRequestPartException] part={}",
                e.getRequestPartName()
        );

        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                Map.of(
                        "reason",
                        "%s 파트는 필수입니다.".formatted(e.getRequestPartName())
                )
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e
    ) {
        log.warn(
                "[HttpMediaTypeNotSupportedException] contentType={}",
                e.getContentType()
        );

        return createResponse(
                ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE,
                Map.of("reason", "지원하지 않는 Content-Type입니다.")
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e
    ) {
        log.warn(
                "[AccessDeniedException] message={}",
                e.getMessage()
        );

        return createResponse(
                ErrorCode.AUTH_FORBIDDEN,
                Map.of("reason", ErrorCode.AUTH_FORBIDDEN.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {
        log.error(
                "[UnhandledException] type={}, message={}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );

        return createResponse(
                ErrorCode.COMMON_INTERNAL_SERVER_ERROR,
                Map.of("reason", ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getMessage())
        );
    }

    private ResponseEntity<ErrorResponse> createResponse(
            ErrorCode errorCode,
            Map<String, Object> details
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ErrorResponse.from(
                                errorCode,
                                details
                        )
                );
    }

    private Map<String, Object> extractFieldErrors(
            java.util.List<FieldError> fieldErrors
    ) {
        return fieldErrors.stream()
                .collect(
                        Collectors.toMap(
                                FieldError::getField,
                                fieldError ->
                                        fieldError.getDefaultMessage() == null
                                                ? "올바르지 않은 값입니다."
                                                : fieldError.getDefaultMessage(),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        )
                );
    }
}