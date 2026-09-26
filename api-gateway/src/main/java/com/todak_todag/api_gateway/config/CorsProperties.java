package com.todak_todag.api_gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		requireAllowedOrigins(allowedOrigins);
	}

	private static void requireAllowedOrigins(List<String> allowedOrigins) {
		if(allowedOrigins == null || allowedOrigins.isEmpty()) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. cors.allowed-origins 에 최소 하나의 Origin 이 필요합니다."
			);
		}

		if(allowedOrigins.contains(CorsConfiguration.ALL)) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. 쿠키 인증을 사용하므로 cors.allowed-origins 에 * 를 쓸 수 없습니다."
			);
		}
	}
}
