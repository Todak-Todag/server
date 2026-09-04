package com.todak_todag.api_gateway.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.todak_todag.api_gateway.config.AuthenticationProperties;
import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;

@DisplayName("JwtTokenParser")
class JwtTokenParserTest {

	/** Base64 로 인코딩된 512bit 시크릿. HS256 과 HS512 를 같은 키로 서명·검증하기 위해 충분히 길게 잡는다. */
	private static final String JWT_SECRET =
			"dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA==";

	private static final byte[] SECRET_BYTES = Decoders.BASE64.decode(JWT_SECRET);

	private static final AuthenticationProperties PROPERTIES = new AuthenticationProperties(
			"AccessToken",
			"access:",
			"sub",
			"role"
	);

	private final JwtTokenParser jwtTokenParser = new JwtTokenParser(JWT_SECRET, PROPERTIES);

	@Test
	@DisplayName("정상 JWT 에서 userId 와 role 을 추출한다")
	void extractsUserIdAndRoleFromValidToken() {
		String token = signedToken(builder -> builder
				.subject("1")
				.claim("role", "USER")
				.expiration(from(Duration.ofMinutes(30))));

		AccessTokenClaims claims = jwtTokenParser.parse(token);

		assertThat(claims.userId()).isEqualTo("1");
		assertThat(claims.role()).isEqualTo("USER");
	}

	@Test
	@DisplayName("서명이 변조된 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsTokenWithTamperedSignature() {
		String token = signedToken(builder -> builder
				.subject("1")
				.claim("role", "USER")
				.expiration(from(Duration.ofMinutes(30))));

		assertThatTokenError(tamperSignature(token), TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("만료된 토큰은 EXPIRED_ACCESS_TOKEN 이다")
	void rejectsExpiredTokenWithExpiredErrorCode() {
		String token = signedToken(builder -> builder
				.subject("1")
				.claim("role", "USER")
				.issuedAt(from(Duration.ofHours(-2)))
				.expiration(from(Duration.ofHours(-1))));

		assertThatTokenError(token, TokenErrorCode.EXPIRED_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("HS512 가 아닌 알고리즘으로 서명된 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsTokenSignedWithUnexpectedAlgorithm() {
		String token = Jwts.builder()
				.subject("1")
				.claim("role", "USER")
				.expiration(from(Duration.ofMinutes(30)))
				.signWith(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"), Jwts.SIG.HS256)
				.compact();

		assertThat(Jwts.parser()
				.verifyWith(hmacKey("HmacSHA256"))
				.build()
				.parseSignedClaims(token)
				.getHeader()
				.getAlgorithm())
				.isEqualTo("HS256");

		assertThatTokenError(token, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("exp 클레임이 없는 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsTokenWithoutExpirationClaim() {
		String token = signedToken(builder -> builder
				.subject("1")
				.claim("role", "USER"));

		assertThatTokenError(token, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("sub 클레임이 없는 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsTokenWithoutSubjectClaim() {
		String token = signedToken(builder -> builder
				.claim("role", "USER")
				.expiration(from(Duration.ofMinutes(30))));

		assertThatTokenError(token, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("role 클레임이 없는 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsTokenWithoutRoleClaim() {
		String token = signedToken(builder -> builder
				.subject("1")
				.expiration(from(Duration.ofMinutes(30))));

		assertThatTokenError(token, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "not-a-jwt" })
	@DisplayName("비어 있거나 JWT 형식이 아닌 토큰은 INVALID_ACCESS_TOKEN 이다")
	void rejectsBlankOrMalformedToken(String token) {
		assertThatTokenError(token, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "!!!not-base64!!!", "c2hvcnQ=" })
	@DisplayName("secret 이 비었거나 Base64 가 아니거나 너무 짧으면 생성 시점에 실패한다")
	void rejectsInvalidSecretAtConstruction(String secret) {
		assertThatThrownBy(() -> new JwtTokenParser(secret, PROPERTIES))
				.isInstanceOf(IllegalStateException.class);
	}

	private void assertThatTokenError(String token, TokenErrorCode expected) {
		assertThatThrownBy(() -> jwtTokenParser.parse(token))
				.isInstanceOf(TokenException.class)
				.extracting(error -> ((TokenException) error).getErrorCode())
				.isEqualTo(expected);
	}

	private static String signedToken(java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
		return customizer.apply(Jwts.builder())
				.signWith(hmacKey("HmacSHA512"), Jwts.SIG.HS512)
				.compact();
	}

	private static SecretKey hmacKey(String algorithm) {
		return new SecretKeySpec(SECRET_BYTES, algorithm);
	}

	/** 헤더와 payload 는 그대로 두고 서명 첫 글자만 바꿔 서명 검증만 깨뜨린다. */
	private static String tamperSignature(String token) {
		int signatureStart = token.lastIndexOf('.') + 1;

		char original = token.charAt(signatureStart);

		char tampered = (original == 'A') ? 'B' : 'A';

		return token.substring(0, signatureStart) + tampered + token.substring(signatureStart + 1);
	}

	private static Date from(Duration offset) {
		return Date.from(Instant.now().plus(offset));
	}

}
