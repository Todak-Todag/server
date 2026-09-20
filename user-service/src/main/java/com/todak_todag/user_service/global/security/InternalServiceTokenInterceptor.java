package com.todak_todag.user_service.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InternalServiceTokenInterceptor implements HandlerInterceptor {

	private static final String TOKEN_HEADER = "Internal-Service-Token";
	
	private static final String LEGACY_HEADER = "X-Internal-Api-Key";
	
	private final JwtDecoder decoder;
	
	private final String legacyKey;
	
	public InternalServiceTokenInterceptor(
			JwtDecoder gatewayTokenDecoder,
			@Value("${internal.key}") String legacyKey
	) {
		this.decoder = gatewayTokenDecoder;
		this.legacyKey = legacyKey;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String token = request.getHeader(TOKEN_HEADER);
		
		if(token != null && !token.isBlank()) {
			try {
				Jwt jwt = decoder.decode(token);
				
				if(!"s2s".equals(jwt.getClaimAsString("token_use"))) {
					log.warn(
							"[User] 내부 인증 서비스 토큰이 아닙니다. uri={}, sub={}",
							request.getRequestURI(),
							jwt.getSubject()
					);
					
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return false;
				}
				
				request.setAttribute("callerService", jwt.getSubject());
				log.debug(
						"[User] 내부 인증 서비스 토큰 검증 성공. 호출자={}, uri={}",
						jwt.getSubject(),
						request.getRequestURI()
				);
				
				return true;
			} catch (JwtException e) {
				log.warn(
						"[User] 내부 인증 서비스 토큰 검증 실패. uri={}, message={}",
						request.getRequestURI(),
						e.getMessage()
				);
				
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}
		}
		
		// --- 마이크로서비스들 내부 서비스 인증구조 변경 후 아래 코드 삭제
		String apiKey = request.getHeader(LEGACY_HEADER);
		if(apiKey != null
			 && !apiKey.isBlank()
			 && MessageDigest.isEqual(legacyKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
			
			log.info("[내부인증] 레거시 정적 키로 통과함(전환 대상). uri={}", request.getRequestURI());
			
			return true;
		}
		
		log.warn(
				"[내부인증] 유효한 서비스 토큰도 정적 키도 없어 요청을 거부합니다. uri={}",
				request.getRequestURI()
		);
		
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return false;
	}
	
	
}
