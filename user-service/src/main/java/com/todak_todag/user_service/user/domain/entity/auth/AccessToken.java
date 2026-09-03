package com.todak_todag.user_service.user.domain.entity.auth;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.redis.core.RedisHash;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@RedisHash(value = "${authentication.access-token.redis-key-prefix}", timeToLive = 1800)
@NoArgsConstructor(force = true)
public class AccessToken {

	@Id
	private final String accessTokenHash;
	
	private final UUID userId;
	
	@PersistenceCreator
	public AccessToken(String accessTokenHash, UUID userId) {
		this.accessTokenHash = Objects.requireNonNull(accessTokenHash);
		this.userId = Objects.requireNonNull(userId);
	}
	
}
