package com.todak_todag.api_gateway.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todak_todag.api_gateway.authentication.ClientContext;
import com.todak_todag.api_gateway.config.InternalJwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@DisplayName("InternalTokenIssuer")
public class InternalTokenIssuerTest {

	private InternalKeyProvider keyProvider;
	
	private InternalTokenIssuer issuer;
	
	private InternalJwtProperties properties;
	
	@BeforeEach
	void setUp() throws NoSuchAlgorithmException {
		properties = new InternalJwtProperties(generateBase64Pem(), "todak-api-gateway", Duration.ofSeconds(60));
		keyProvider = new InternalKeyProvider(properties);
		issuer = new InternalTokenIssuer(keyProvider, properties);
	}
	
	@Test
	@DisplayName("발급한 토큰은 같은 Provider 의 공개키로 서명 검증이 되고, 클레임이 전부 채워진다.")
	void issuedTokenIsVerifiableAndContainsExceptedClaims() {
		ClientContext client = new ClientContext("3f1c2b7e-8d5a-4e2f-9a61-0c7b5d4e8f12", "PATIENT");
		
		String token = issuer.issue(client, "schedule-service");
		
		Jws<Claims> parsed = Jwts.parser()
				.verifyWith(keyProvider.getPublicKey())
				.build()
				.parseSignedClaims(token);
		
		Claims claims = parsed.getPayload();
		
		assertThat(parsed.getHeader().getKeyId()).isEqualTo(keyProvider.getKeyId());
		assertThat(claims.getIssuer()).isEqualTo("todak-api-gateway");
		assertThat(claims.getSubject()).isEqualTo(client.userId());
		assertThat(claims.get("role", String.class)).isEqualTo("PATIENT");
		assertThat(claims.getAudience()).containsExactly("schedule-service");
		assertThat(claims.getId()).isNotBlank();
	}
	
	@Test
	@DisplayName("만료 시각은 발급 시각으로부터 설정된 ttl 만큼 뒤다")
	void expirationRespectsConfiguredTtl() {
		ClientContext client = new ClientContext("3f1c2b7e-8d5a-4e2f-9a61-0c7b5d4e8f12", "PATIENT");

		String token = issuer.issue(client, "schedule-service");

		Claims claims = Jwts.parser()
				.verifyWith(keyProvider.getPublicKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();

		long actualSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;

		assertThat(actualSeconds).isEqualTo(properties.ttl().toSeconds());
	}
	
	@Test
	@DisplayName("다른 개인키의 공개키로 검증하면 실패한다")
	void rejectsVerificationWithUnrelatedPublicKey() throws NoSuchAlgorithmException {
		ClientContext client = new ClientContext("3f1c2b7e-8d5a-4e2f-9a61-0c7b5d4e8f12", "PATIENT");
		String token = issuer.issue(client, "schedule-service");

		InternalKeyProvider unrelatedProvider = new InternalKeyProvider(
				new InternalJwtProperties(generateBase64Pem(), "todak-api-gateway", Duration.ofSeconds(60)));

		assertThatThrownBy(() -> Jwts.parser()
				.verifyWith(unrelatedProvider.getPublicKey())
				.build()
				.parseSignedClaims(token))
				.isInstanceOf(JwtException.class);
	}
	
	private static String generateBase64Pem() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();

		String innerBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		String pemText = "-----BEGIN PRIVATE KEY-----\n" + innerBase64 + "\n-----END PRIVATE KEY-----\n";

		return Base64.getEncoder().encodeToString(pemText.getBytes());
	}
}
