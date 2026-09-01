package com.todak_todag.api_gateway.store;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class TokenHashGenerator {

	private static final String HASH_ALGORITHM = "SHA-256";
	
	public String generate(String token) {
		if(token == null || token.isEmpty()) {
			throw new IllegalArgumentException("해시할 액세스 토큰이 비어있습니다.");
		}
		
		MessageDigest messageDigest = createMessageDigest();
		byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
		byte[] digest = messageDigest.digest(tokenBytes);
		
		return HexFormat.of().formatHex(digest);
	}
	
	private MessageDigest createMessageDigest() {
		try {
			return MessageDigest.getInstance(HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(
					HASH_ALGORITHM + ": 지원하지 않는 알고리즘입니다.",
					e
			);
		}
	}
}
