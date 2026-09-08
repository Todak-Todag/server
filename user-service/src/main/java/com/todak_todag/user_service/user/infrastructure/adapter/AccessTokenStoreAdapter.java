package com.todak_todag.user_service.user.infrastructure.adapter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AccessTokenStoreAdapter implements TokenStorePort {

	private final Duration accessTokenTtl;

	private final TokenPort tokenPort;

	private final String accessKeyPrefix;

	private final RedisTemplate<String, String> redisTemplate;

	public AccessTokenStoreAdapter(
			@Value("${authentication.access-token.redis-key-prefix}") String accessKeyPrefix,
			@Value("${jwt.access.max-age}") Duration accessTokenTtl,
			RedisTemplate<String, String> redisTemplate,
			TokenPort tokenPort
	) {
		if(accessKeyPrefix == null || accessKeyPrefix.isBlank()) {
			log.error("[User] 서버 구동 실패 Redis Key Prefix 설정 값이 비어있습니다.");
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 Redis Key Prefix 설정 값이 비어 있습니다.");
		}
		
		if(accessTokenTtl == null) {
			log.error("[User] 서버 구동 실패 Access Token TTL 설정 값이 비어있습니다.");

			throw new IllegalArgumentException("[User] 서버 구동 실패 Access Token TTL 설정 값이 비어있습니다.");
		}
		
		if(accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
			log.error("[User] 서버 구동 실패 Access Token TTL 설정 값은 0 이하일 수 없습니다.");
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 Access Token TTL 설정 값은 0 이하일 수 없습니다.");
		}
		
		this.accessKeyPrefix = accessKeyPrefix;
		this.redisTemplate = redisTemplate;
		this.tokenPort = tokenPort;
		this.accessTokenTtl = accessTokenTtl;
	}

	@Override
	public void storeAccessToken(String accessToken, String jwtAccessToken) {
		redisTemplate.opsForValue().set(
				accessKeyPrefix + tokenPort.hashToken(accessToken),
				jwtAccessToken,
				accessTokenTtl
		);
	}

	@Override
	public void deleteAccessToken(String accessToken) {
		if(accessToken == null || accessToken.isBlank()) {
			return;
		}
		
		String key = accessKeyPrefix + tokenPort.hashToken(accessToken);
		redisTemplate.delete(key);
	}

}
