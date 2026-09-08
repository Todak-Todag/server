package com.todak_todag.social_worker_service.global.exception;

import com.todak_todag.social_worker_service.global.response.ErrorResponse;
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

import java.util.LinkedHashMap;
import java.util.List;
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

        Map<String, Object> details =
                e.getDetails() == null || e.getDetails().isEmpty()
                        ? Map.of("reason", e.getMessage())
                        : e.getDetails();

        log.warn(
                "[SocialWorker][BusinessException] code={}, message={}",
                errorCode.getCode(),
                e.getMessage()
        );

        return createResponse(
                errorCode,
                details
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            RuntimeException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of(
                        "reason",
                        resolveMessage(
                                e.getMessage(),
                                ErrorCode.COMMON_INVALID_REQUEST.getMessage()
                        )
                )
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(
            NoSuchElementException e
    ) {
        return createResponse(
                ErrorCode.COMMON_NOT_FOUND,
                Map.of(
                        "reason",
                        resolveMessage(
                                e.getMessage(),
                                ErrorCode.COMMON_NOT_FOUND.getMessage()
                        )
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of(
                        "reason",
                        "%s의 형식이 올바르지 않습니다."
                                .formatted(e.getName())
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                extractFieldErrors(
                        e.getBindingResult().getFieldErrors()
                )
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                extractFieldErrors(
                        e.getBindingResult().getFieldErrors()
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_REQUEST,
                Map.of(
                        "reason",
                        "요청 본문의 형식이 올바르지 않습니다."
                )
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        return createResponse(
                ErrorCode.COMMON_INVALID_INPUT_VALUE,
                Map.of(
                        "reason",
                        "%s 파라미터는 필수입니다."
                                .formatted(e.getParameterName())
                )
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e
    ) {
        return createResponse(
                ErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE,
                Map.of(
                        "reason",
                        "지원하지 않는 Content-Type입니다."
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e
    ) {
        return createResponse(
                ErrorCode.AUTH_FORBIDDEN,
                Map.of(
                        "reason",
                        ErrorCode.AUTH_FORBIDDEN.getMessage()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {
        log.error(
                "[SocialWorker][UnhandledException] type={}, message={}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );

        return createResponse(
                ErrorCode.COMMON_INTERNAL_SERVER_ERROR,
                Map.of(
                        "reason",
                        ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getMessage()
                )
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
            List<FieldError> fieldErrors
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

    private String resolveMessage(
            String message,
            String defaultMessage
    ) {
        return message == null || message.isBlank()
                ? defaultMessage
                : message;
    }
}
