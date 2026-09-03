package com.todak_todag.user_service.user.infrastructure.adapter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.AccessTokenStorePort;
import com.todak_todag.user_service.user.application.port.TokenPort;

@Component
public class AccessTokenStoreAdapter implements AccessTokenStorePort {

	private static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(7);

	private final TokenPort tokenPort;

	private final String accessKeyPrefix;

	private final RedisTemplate<String, String> redisTemplate;

	public AccessTokenStoreAdapter(
			@Value("${authentication.access-token.redis-key-prefix}") String accessKeyPrefix,
			RedisTemplate<String, String> redisTemplate,
			TokenPort tokenPort
	) {
		this.accessKeyPrefix = accessKeyPrefix;
		this.redisTemplate = redisTemplate;
		this.tokenPort = tokenPort;
	}

	@Override
	public void storeAccessToken(String accessToken, String jwtAccessToken) {
		redisTemplate.opsForValue().set(
				accessKeyPrefix + tokenPort.hashToken(accessToken),
				jwtAccessToken,
				ACCESS_TOKEN_TTL
		);
	}

}
