package com.todak_todag.schedule_service.global.security;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
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
public class InternalResponseInterceptor implements HandlerInterceptor {

    private final String internalApiKey;

    public InternalResponseInterceptor(@Value("${internal.key}") String internalApiKey) {

        if(internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalArgumentException("internal.key 는 서버 구동에 필요한 설정입니다.");
        }

        this.internalApiKey = internalApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestApiKey = request.getHeader(InternalHeader.INTERNAL_KEY);

        if(requestApiKey == null || requestApiKey.isBlank() || !matches(requestApiKey)) {
            log.warn(
                    "[Schedule] Internal API 헤더 누락 uri={}, remoteAddr={}",
                    request.getRequestURI(),
                    request.getRemoteAddr()
            );

            throw new BusinessException(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
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
