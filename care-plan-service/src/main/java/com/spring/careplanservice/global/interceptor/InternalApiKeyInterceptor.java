package com.spring.careplanservice.global.interceptor;

import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import com.spring.careplanservice.global.security.InternalHeader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
public class InternalApiKeyInterceptor implements HandlerInterceptor {
    private final String internalApiKey;

    public InternalApiKeyInterceptor(
            @Value("${internal.key}") String internalApiKey
    ) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "internal.key 는 서버 구동에 필요한 설정입니다."
            );
        }

        this.internalApiKey = internalApiKey;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String requestApiKey = request.getHeader(InternalHeader.INTERNAL_KEY);

        if (
                requestApiKey == null
                        || requestApiKey.isBlank()
                        || !matches(requestApiKey)
        ) {
            log.warn(
                    "[CarePlan] 내부 API 인증 실패 uri={} remoteAddr={}",
                    request.getRequestURI(),
                    request.getRemoteAddr()
            );

            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED_INTERNAL_REQUEST
            );
        }

        return true;
    }

    private boolean matches(String requestApiKey) {
        return MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                requestApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
