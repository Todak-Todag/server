package com.todak_todag.user_service.user.infrastructure.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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
	
	private final String userKeyPrefix;

	private final RedisTemplate<String, String> redisTemplate;

	public AccessTokenStoreAdapter(
			@Value("${authentication.access-token.redis-key-prefix}") String accessKeyPrefix,
			@Value("${authentication.user.redis-key-prefix}") String userKeyPrefix,
			@Value("${jwt.access.max-age}") Duration accessTokenTtl,
			RedisTemplate<String, String> redisTemplate,
			TokenPort tokenPort
	) {
		if(accessKeyPrefix == null
			|| accessKeyPrefix.isBlank()
			|| userKeyPrefix == null
			|| userKeyPrefix.isBlank()
		) {
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
		this.userKeyPrefix = userKeyPrefix;
		this.redisTemplate = redisTemplate;
		this.tokenPort = tokenPort;
		this.accessTokenTtl = accessTokenTtl;
	}

	@Override
	public void storeAccessToken(UUID userId, String accessToken, String jwtAccessToken) {
		storeWithUserIndex(userId, accessToken, jwtAccessToken, accessTokenTtl);
	}

	@Override
	public void deleteAccessToken(UUID userId, String accessToken) {
		if(accessToken == null || accessToken.isBlank()) {
			return;
		}
		
		String tokenHash = tokenPort.hashToken(accessToken);
		
		redisTemplate.delete(accessKeyPrefix + tokenHash);
		
		redisTemplate.opsForSet().remove(userKeyPrefix + userId, tokenHash);
	}

	@Override
	public void storeAccessTokenTemp(UUID userId, String accessToken, String jwtAccessToken, Duration ttl) {
		storeWithUserIndex(userId, accessToken, jwtAccessToken, ttl);
	}

	@Override
	public void revokeAllSessions(UUID userId) {
		String userKey = userKeyPrefix + userId;
		
		Set<String> tokenHashes = redisTemplate.opsForSet().members(userKey);
		
		// 실제 토큰을 먼저 지운다. 인덱스를 먼저 지우면 삭제 실패 시 대상을 다시 찾을 수 없다.
		if(tokenHashes != null && !tokenHashes.isEmpty()) {
			List<String> accessKeys = tokenHashes.stream()
					.map(tokenHash -> accessKeyPrefix + tokenHash)
					.toList();
			
			redisTemplate.delete(accessKeys);
		}
		
		redisTemplate.delete(userKey);
		
		log.info(
				"[User] 사용자 세션 전체 무효화 userId={}, revokedCount={}",
				userId,
				tokenHashes == null ? 0 : tokenHashes.size()
		);
	}
	
	// AccessToken 저장과 사용자별 역인덱스 등록을 MULTI/EXEC 로 묶는다.
	// 둘 중 하나만 남으면 '무효화할 수 없는 토큰' 또는 '가리킬 대상이 없는 해시' 가 된다.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void storeWithUserIndex(UUID userId, String accessToken, String jwtAccessToken, Duration ttl) {
		String tokenHash = tokenPort.hashToken(accessToken);
		
		String accessKey = accessKeyPrefix + tokenHash;
		
		String userKey = userKeyPrefix + userId;
		
		redisTemplate.execute(new SessionCallback<Void>() {
			@Override
			public Void execute(RedisOperations operations) {
				operations.multi();
				
				operations.opsForValue().set(accessKey, jwtAccessToken, ttl);
				
				operations.opsForSet().add(userKey, tokenHash);
				
				// 3분짜리 임시 토큰이 인덱스 TTL 을 깎지 않도록 인덱스는 항상 AccessToken 최대 수명으로 맞춘다.
				operations.expire(userKey, accessTokenTtl);
				
				operations.exec();
				
				return null;
			}
		});
	}

}
