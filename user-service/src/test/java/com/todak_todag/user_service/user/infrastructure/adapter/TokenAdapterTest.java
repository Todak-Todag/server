package com.todak_todag.user_service.user.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todak_todag.user_service.global.common.UserRole;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@DisplayName("TokenAdapter 단위테스트")
class TokenAdapterTest {

	private static final Duration ACCESS_EXPIRATION = Duration.ofMinutes(30);

	// 디코딩 시 정확히 length 바이트가 되는 base64 SecretKey 문자열을 만든다
	private static String secretKeyOfDecodedLength(int length) {
		byte[] raw = new byte[length];
		return Base64.getEncoder().encodeToString(raw);
	}

	@Test
	@DisplayName("secretKey 가 null 이면 IllegalArgumentException 이 발생한다")
	void constructorTest_fail_secretKeyNull() {
		assertThatThrownBy(() -> new TokenAdapter(ACCESS_EXPIRATION, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("secretKey 가 blank 이면 IllegalArgumentException 이 발생한다")
	void constructorTest_fail_secretKeyBlank() {
		assertThatThrownBy(() -> new TokenAdapter(ACCESS_EXPIRATION, "   "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("인코딩 문자열 길이는 64자 이상이지만 디코딩하면 64바이트 미만이면 IllegalArgumentException 이 발생한다")
	void constructorTest_fail_encodedLengthLongButDecodedLengthShort() {
		// 디코딩 시 48바이트 -> base64 인코딩 길이는 64자 (패딩 없음)
		String secretKey = secretKeyOfDecodedLength(48);
		assertThat(secretKey.length()).isGreaterThanOrEqualTo(64);

		assertThatThrownBy(() -> new TokenAdapter(ACCESS_EXPIRATION, secretKey))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("디코딩 시 정확히 64바이트(HS512 최소 요건)인 secretKey 면 정상 생성된다")
	void constructorTest_success_decodedLengthExactly64() {
		String secretKey = secretKeyOfDecodedLength(64);

		assertThatCode(() -> new TokenAdapter(ACCESS_EXPIRATION, secretKey))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("accessExpiration 이 null 이면 IllegalArgumentException 이 발생한다")
	void constructorTest_fail_accessExpirationNull() {
		String secretKey = secretKeyOfDecodedLength(64);

		assertThatThrownBy(() -> new TokenAdapter(null, secretKey))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("accessExpiration 이 음수이면 IllegalArgumentException 이 발생한다")
	void constructorTest_fail_accessExpirationNegative() {
		String secretKey = secretKeyOfDecodedLength(64);

		assertThatThrownBy(() -> new TokenAdapter(Duration.ofMinutes(-1), secretKey))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Jws<io.jsonwebtoken.Claims> parse(String secretKey, String jwt) {
		SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(jwt);
	}

	@Test
	@DisplayName("생성된 JWT 의 header algorithm 은 HS512 이다")
	void createJwtAccessTokenTest_algorithmIsHs512() {
		String secretKey = secretKeyOfDecodedLength(64);
		TokenAdapter tokenAdapter = new TokenAdapter(ACCESS_EXPIRATION, secretKey);

		String jwt = tokenAdapter.createJwtAccessToken(UUID.randomUUID(), UserRole.PATIENT);

		JwsHeader header = parse(secretKey, jwt).getHeader();
		assertThat(header.getAlgorithm()).isEqualTo("HS512");
	}

	@Test
	@DisplayName("생성된 JWT 의 subject 는 전달한 userId 와 일치한다")
	void createJwtAccessTokenTest_subjectIsUserId() {
		String secretKey = secretKeyOfDecodedLength(64);
		TokenAdapter tokenAdapter = new TokenAdapter(ACCESS_EXPIRATION, secretKey);

		UUID userId = UUID.randomUUID();
		String jwt = tokenAdapter.createJwtAccessToken(userId, UserRole.PATIENT);

		assertThat(parse(secretKey, jwt).getPayload().getSubject()).isEqualTo(userId.toString());
	}

	@Test
	@DisplayName("생성된 JWT 의 role claim 은 전달한 UserRole 과 일치한다")
	void createJwtAccessTokenTest_roleClaimMatches() {
		String secretKey = secretKeyOfDecodedLength(64);
		TokenAdapter tokenAdapter = new TokenAdapter(ACCESS_EXPIRATION, secretKey);

		String jwt = tokenAdapter.createJwtAccessToken(UUID.randomUUID(), UserRole.SOCIAL_WORKER);

		String role = parse(secretKey, jwt).getPayload().get("role", String.class);
		assertThat(role).isEqualTo(UserRole.SOCIAL_WORKER.name());
	}

	@Test
	@DisplayName("생성된 JWT 의 만료 시간과 발급 시간의 간격은 설정된 accessExpiration 과 일치한다")
	void createJwtAccessTokenTest_expirationMatchesAccessExpiration() {
		String secretKey = secretKeyOfDecodedLength(64);
		TokenAdapter tokenAdapter = new TokenAdapter(ACCESS_EXPIRATION, secretKey);

		String jwt = tokenAdapter.createJwtAccessToken(UUID.randomUUID(), UserRole.PATIENT);

		io.jsonwebtoken.Claims claims = parse(secretKey, jwt).getPayload();
		long diffMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

		assertThat(diffMillis).isEqualTo(ACCESS_EXPIRATION.toMillis());
	}
}
