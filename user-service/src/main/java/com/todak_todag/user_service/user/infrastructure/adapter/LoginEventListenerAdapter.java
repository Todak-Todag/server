package com.todak_todag.user_service.user.infrastructure.adapter;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.todak_todag.user_service.user.application.event.LoginSuccessEvent;
import com.todak_todag.user_service.user.application.port.LoginEventListenerPort;
import com.todak_todag.user_service.user.application.port.TokenPort;

@Component
public class LoginEventListenerAdapter implements LoginEventListenerPort {

	private static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(7);
	
	private final TokenPort tokenPort;
	
	private final String accessKeyPrefix;
	
	private final RedisTemplate<String, String> redisTemplate;
	
	public LoginEventListenerAdapter(
			@Value("${authentication.access-token.redis-key-prefix}") String accessKeyPrefix,
			RedisTemplate<String, String> redisTemplate,
			TokenPort tokenPort
	) {
		this.accessKeyPrefix = accessKeyPrefix;
		this.redisTemplate = redisTemplate;
		this.tokenPort = tokenPort;
	}
	
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserLogin(LoginSuccessEvent event) {
		redisTemplate.opsForValue().set(
				accessKeyPrefix + tokenPort.hashToken(event.accessToken()),
				event.jwtAccessToken(),
				ACCESS_TOKEN_TTL
		);
	}
	
}
