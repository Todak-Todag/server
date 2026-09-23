package com.todak_todag.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authentication.access-token")
public record AuthenticationProperties(
		String cookieName,
		String redisKeyPrefix,
		String userIdClaim,
		String roleClaim
) {

	public AuthenticationProperties {
		requireText(cookieName, "cookie-name");
		requireText(redisKeyPrefix, "key-prefix");
		requireText(userIdClaim, "user-id-claim");
		requireText(roleClaim, "role-claim");
	}
	
	private static void requireText(String value, String propertyName) {
		if(value == null || value.isBlank()) {
			throw new IllegalArgumentException(
					"authentication.access-token." + propertyName + " 값이 설정되어야 합니다."
			);
		}
	}
	
}
