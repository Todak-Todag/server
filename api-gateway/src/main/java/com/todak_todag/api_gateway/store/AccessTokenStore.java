package com.todak_todag.api_gateway.store;

import java.util.regex.Pattern;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.todak_todag.api_gateway.config.AuthenticationProperties;
import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class AccessTokenStore {

	private static final Pattern SHA_256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
	
	private final ReactiveStringRedisTemplate redisTemplate;
	
  private final AuthenticationProperties authenticationProperties;
	
	public Mono<String> findByHash(String tokenHash) {
		if(!isValidTokenHash(tokenHash)) {
			return Mono.error(new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN));
		}
		
		String redisKey = authenticationProperties.redisKeyPrefix() + tokenHash;
		
		return redisTemplate.opsForValue()
				.get(redisKey)
				.filter(token -> !token.isBlank());
	}
	
	private boolean isValidTokenHash(String tokenHash) {
		return tokenHash != null && SHA_256_HEX_PATTERN.matcher(tokenHash).matches();
	}
	
}
