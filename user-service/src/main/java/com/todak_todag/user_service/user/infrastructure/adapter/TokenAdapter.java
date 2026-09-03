package com.todak_todag.user_service.user.infrastructure.adapter;

import java.security.SecureRandom;
import java.time.Duration;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.TokenPort;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenAdapter implements TokenPort {

	private static final int PHANTOM_TOKEN_LENGTH = 64;
	
	private static final String PHANTOM_TOKEN_MATERIAL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	
	private final SecureRandom secureRandom = new SecureRandom();
	
	private final Duration accessExpiration;
	
	private final SecretKey key;
	
	public TokenAdapter(
			@Value("${jwt.access.expiration}") Duration accessExpiration,
			@Value("${jwt.secret}") String secretKey
	) {
		if(accessExpiration == null || accessExpiration.isNegative()) {
			log.error(
					"[User] AccessToken 만료 시간 설정 실패 expiresAt={}/s",
					accessExpiration.getSeconds()
			);
			throw new IllegalArgumentException("서버 구동에 AccessToken 만료 시간이 설정되어야 합니다.");
		}
		
		if(secretKey == null || secretKey.isBlank()) {
			log.error("[User] JWT Secret Key 값이 비어있습니다.");
			throw new IllegalArgumentException("서버 구동에 JWT Secret Key가 필요합니다.");
		}
		
		this.accessExpiration = accessExpiration;
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
	}
	
	
	
}
