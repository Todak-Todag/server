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
public class PhantomToken {

	@Id
	private final String phantomTokenHash;
	
	private final UUID userId;
	
	@PersistenceCreator
	public PhantomToken(String phantomTokenHash, String accessToken, UUID userId) {
		this.phantomTokenHash = Objects.requireNonNull(phantomTokenHash);
		this.userId = Objects.requireNonNull(userId);
	}
	
}
