package com.todak_todag.user_service.user.infrastructure.adapter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.application.port.TokenPort;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenAdapter implements TokenPort {

	// AccessToken 문자열 자릿수 - 256bit 엔트로피
	private static final int ACCESS_TOKEN_LENGTH = 32;
	
	// 문자열 재료
	private static final String ACCESS_TOKEN_MATERIAL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	
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

	// 암호학적 안전한 랜덤 난수로 32자리수 무작위 문자열 토큰 생성 - AccessToken
	@Override
	public String createPhantomToken() {
		StringBuilder token = new StringBuilder(ACCESS_TOKEN_LENGTH);
		for(int i = 0; i < ACCESS_TOKEN_LENGTH; i++) {
			int index = secureRandom.nextInt(ACCESS_TOKEN_MATERIAL.length());
			token.append(ACCESS_TOKEN_MATERIAL.charAt(index));
		}
		return token.toString();
	}

	// AccessToken을 SHA-256 으로 해시
	@Override
	public String hashPhantomToken(String phantomToken) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(phantomToken.getBytes(StandardCharsets.UTF_8));
			
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			log.error("[User] 토큰 해시를 만들 알고리즘이 존재하지 않습니다.", e);
			
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.");
		}
	}
	
	// JWT AccessToken 만들기
	@Override
	public String createAccessToken(UUID userId, UserRole role) {
		Instant now = Instant.now();
		Date iss = Date.from(now);
		Date exp = Date.from(now.plus(accessExpiration));
		
		String accessToken = Jwts.builder()
				.subject(userId.toString())
				.issuedAt(iss)
				.expiration(exp)
				.signWith(key)
				.compact();
		
		return accessToken;
	}
	
	
	
}
