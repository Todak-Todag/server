package com.todak_todag.api_gateway.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.todak_todag.api_gateway.config.InternalJwtProperties;

@DisplayName("InternalKeyProvider")
public class InternalKeyProviderTest {

	private static String validBase64Pem;
	
	@BeforeAll
	static void generateKeyPair() throws NoSuchAlgorithmException {
		validBase64Pem = generateBase64Pem();
	}

	@Test
	@DisplayName("유효한 PEM 이면 개인키.공개키.kid 를 모두 계산한다.")
	void success_1() {
		InternalKeyProvider provider = new InternalKeyProvider(properties(validBase64Pem));
		
		assertThat(provider.getPrivateKey()).isNotNull();
		assertThat(provider.getPublicKey()).isNotNull();
		assertThat(provider.getKeyId()).isNotNull();
	}
	
	@Test
	@DisplayName("같은 개인키로 만들면 항상 같은 kid 가 나와야 한다.")
	void success_2() {
		InternalKeyProvider first = new InternalKeyProvider(properties(validBase64Pem));
		InternalKeyProvider second = new InternalKeyProvider(properties(validBase64Pem));
		
		assertThat(first.getKeyId()).isEqualTo(second.getKeyId());
	}
	
	@Test
	@DisplayName("다른 개인키로 만들면 다른 kid 가 나와야 한다.")
	void success_3() throws NoSuchAlgorithmException {
		InternalKeyProvider first = new InternalKeyProvider(properties(validBase64Pem));
		InternalKeyProvider second = new InternalKeyProvider(properties(generateBase64Pem()));
		
		assertThat(first.getKeyId()).isNotEqualTo(second.getKeyId());
	}
	
	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = { "not-base64!!!", "c2hvcnQ=" })
	@DisplayName("private-key 가 유효한 PKCS8 PEM 이 아니면 생성 시점에 실패한다")
	void success_4(String malformedKey) {
		assertThatThrownBy(() -> new InternalKeyProvider(properties(malformedKey)))
				.isInstanceOf(IllegalStateException.class);
	}
	
	private InternalJwtProperties properties(String privateKey) {
		return new InternalJwtProperties(privateKey, "todak-api-gateway", Duration.ofSeconds(60));
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
