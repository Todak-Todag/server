package com.todak_todag.api_gateway.token;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.todak_todag.api_gateway.config.InternalJwtProperties;

import io.jsonwebtoken.security.Jwks;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/*
 * privateKey를 RSAPrivateKey 객체로 파싱
 * RSAPublicKey를 계산 -> 공개키
 * 만들어진 공개키로 kid 만든다.
 * 
 * Gateway가 뜰 때 딱 한 번 계산이 이루어지고 필드에 저장해둔다.
 * 토큰 서명과 JWKS 응답을 만들 때 필드의 값을 계속 쓰게된다.
 */
@Slf4j
@Getter
@Component
public class InternalKeyProvider {

	private final RSAPrivateKey privateKey;
	
	private final RSAPublicKey publicKey;
	
	private final String keyId;
	
	public InternalKeyProvider(InternalJwtProperties properties) {
		this.privateKey = loadPrivateKey(properties.privateKey());
		this.publicKey = derivePublicKey(this.privateKey);
		this.keyId = computeKeyId(this.publicKey);
	}
	
	private RSAPrivateKey loadPrivateKey(String base64Pem) {
		try {
			String pem = new String(Base64.getDecoder().decode(base64Pem))
					.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
					.replaceAll("\\s", "");
			
			byte[] decoded = Base64.getDecoder().decode(pem);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			
			return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			log.error("[Gateway] 서버 구동 실패. private-key 로딩에 실패했습니다.", e);
			
			throw new IllegalStateException("private-key는 유효한 PKCS8 PEM 을 Base64 인코딩한 값이어야 합니다.", e);
		}
	}
	
	private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
		try {
			RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			
			return (RSAPublicKey) keyFactory.generatePublic(new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
		} catch (GeneralSecurityException e) {
			log.error("[Gateway] 서버 구동 실패. 공개키 계산에 실패하였습니다.", e);
			
			throw new IllegalStateException("개인키로 공개키를 계산할 수 없습니다.", e);
		}
	}
	
	private String computeKeyId(RSAPublicKey publicKey) {
		return Jwks.builder()
				.key(publicKey)
				.idFromThumbprint()
				.build()
				.getId();
	}
}
