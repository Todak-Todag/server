package com.todak_todag.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authentication.refresh-token")
public record RefreshTokenProperties(
		String cookieName
) {

	public RefreshTokenProperties {
		requireText(cookieName, "cookie-name");
	}
	
	private static void requireText(String value, String propertyName) {
		if(value == null || value.isBlank()) {
			throw new IllegalArgumentException(
					"authentication.refresh-token." + propertyName + " 값이 설정되어야 합니다."
			);
		}
	}
}
