package com.todak_todag.user_service.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

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

		// 헤더 자체가 없는 것과 키가 틀린 것은 대응이 완전히 다르므로 분리해서 남긴다.
		// 헤더 없음 : 호출 측 서비스의 설정 누락일 가능성이 높다.
		if(requestApiKey == null || requestApiKey.isBlank()) {
			log.warn(
					"[User] 내부 API 키 헤더 없이 내부 전용 경로가 호출되었습니다. uri={}, remoteAddr={}",
					request.getRequestURI(),
					request.getRemoteAddr()
			);

			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
		}

		// 키 불일치 : 게이트웨이가 /internal/** 을 denyAll 로 막고 있으므로,
		//            이 요청은 게이트웨이를 우회해 서비스 포트로 직접 들어온 것이다.
		if(!matches(requestApiKey)) {
			log.warn(
					"[User] 잘못된 내부 API 키로 내부 전용 경로가 호출되었습니다. uri={}, remoteAddr={}",
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
