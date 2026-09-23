package com.todak_todag.api_gateway.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-jwt")
public record InternalJwtProperties(
		String privateKey,
		String issuer,
		Duration ttl
) {

	public InternalJwtProperties {
		requireText(privateKey, "private-key");
		requireText(issuer, "issuer");
		requireTtl(ttl);
	}
	
	private static void requireText(String value, String propertyName) {
		if(value == null || value.isBlank()) {
			throw new IllegalArgumentException("[Gateway] 서버 구동 실패. internal-jwt." + propertyName + " 값이 설정되어야 합니다.");
		}
	}
	
	private static void requireTtl(Duration ttl) {
		if(ttl == null || ttl.isNegative() || ttl.isZero()) {
			throw new IllegalArgumentException("[Gateway] 서버 구동 실패. internal-jwt.ttl 값은 0보다 커야 합니다.");
		}
	}
}
