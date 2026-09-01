package com.todak_todag.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.store.AccessTokenStore;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import reactor.core.publisher.Mono;

/**
 * Cookie 검증 실패가 항상 같은 형태의 오류 JSON 으로 나가는지 실제 Security filter chain 을 통해 확인한다.
 * 운영 Redis / Eureka 에 의존하지 않도록 AccessTokenStore 만 대역으로 바꾸고 discovery 는 꺼 둔다.
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"jwt.secret=dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA==",
				"internal.key=test-internal-key",
				"authentication.access-token.redis-key-prefix=access:",
				"eureka.client.enabled=false",
				"eureka.client.register-with-eureka=false",
				"eureka.client.fetch-registry=false",
				"management.tracing.enabled=false"
		}
)
@DisplayName("Security 오류 응답")
class SecurityErrorResponseTest {

	private static final String COOKIE_NAME = "AccessToken";

	private static final String PROTECTED_PATH = "/api/v1/users/1";

	private static final byte[] SECRET_BYTES = Decoders.BASE64.decode(
			"dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA=="
	);

	@Autowired
	private Environment environment;

	@MockitoBean
	private AccessTokenStore accessTokenStore;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		// Boot 4 는 WebTestClient 자동 구성을 별도 test 스타터로 분리했으므로 직접 만든다.
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + environment.getRequiredProperty("local.server.port"))
				.build();
	}

	@Test
	@DisplayName("보호 API 에 Cookie 가 없으면 401 UNAUTHORIZED JSON 이다")
	void returnsUnauthorizedWhenCookieIsAbsent() {
		webTestClient.get()
				.uri(PROTECTED_PATH)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.code").isEqualTo(TokenErrorCode.UNAUTHORIZED.getCode())
				.jsonPath("$.message").isEqualTo(TokenErrorCode.UNAUTHORIZED.getMessage());
	}

	@Test
	@DisplayName("저장소에 없는 토큰은 401 INVALID_ACCESS_TOKEN JSON 이다")
	void returnsInvalidAccessTokenWhenStoreHasNoToken() {
		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.empty());

		expectTokenError(validToken(), HttpStatus.UNAUTHORIZED, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("저장된 토큰과 다른 Cookie 토큰은 401 INVALID_ACCESS_TOKEN JSON 이다")
	void returnsInvalidAccessTokenWhenStoredTokenDiffers() {
		// exp/iat 는 초 단위라 같은 조건으로 만들면 문자열이 같아진다. subject 를 달리해 확실히 다른 토큰을 만든다.
		given(accessTokenStore.findByHash(anyString()))
				.willReturn(Mono.just(token("999", Duration.ofMinutes(30))));

		expectTokenError(validToken(), HttpStatus.UNAUTHORIZED, TokenErrorCode.INVALID_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("만료된 토큰은 401 EXPIRED_ACCESS_TOKEN JSON 이다")
	void returnsExpiredAccessTokenWhenTokenIsExpired() {
		String expiredToken = expiredToken();

		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.just(expiredToken));

		expectTokenError(expiredToken, HttpStatus.UNAUTHORIZED, TokenErrorCode.EXPIRED_ACCESS_TOKEN);
	}

	@Test
	@DisplayName("저장소 장애는 503 AUTHENTICATION_SERVICE_UNAVAILABLE JSON 이다")
	void returnsServiceUnavailableWhenStoreFails() {
		given(accessTokenStore.findByHash(anyString()))
				.willReturn(Mono.error(new RedisConnectionFailureException("redis unavailable")));

		expectTokenError(
				validToken(),
				HttpStatus.SERVICE_UNAVAILABLE,
				TokenErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
		);
	}

	/**
	 * /internal/** 은 denyAll 이지만 protectedRequestMatcher 의 공개 목록에도 들어 있어
	 * AuthenticationWebFilter 가 아예 돌지 않는다. 그래서 Cookie 를 실어 보내도 인증 객체가 없고,
	 * ExceptionTranslationWebFilter 는 AccessDeniedException 을 AccessDeniedHandler(403) 가 아니라
	 * AuthenticationEntryPoint(401) 로 넘긴다. 차단은 되지만 응답 코드는 401 이다.
	 */
	@Test
	@DisplayName("denyAll 로 막힌 /internal/** 은 Cookie 를 실어 보내도 401 UNAUTHORIZED 로 차단된다")
	void blocksInternalPathWithUnauthorized() {
		String token = validToken();

		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.just(token));

		webTestClient.get()
				.uri("/internal/anything")
				.cookie(COOKIE_NAME, token)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.code").isEqualTo(TokenErrorCode.UNAUTHORIZED.getCode());
	}

	@Test
	@DisplayName("공개 경로는 Cookie 가 없어도 401 로 막히지 않는다")
	void doesNotRejectPublicPathsWithoutCookie() {
		assertPublicPathIsNotUnauthorized("/api/v1/regions/1");
		assertPublicPathIsNotUnauthorized("/api/v1/consent-documents/1");
		assertPublicPathIsNotUnauthorized("/actuator/health");
	}

	private void assertPublicPathIsNotUnauthorized(String path) {
		webTestClient.get()
				.uri(path)
				.exchange()
				.expectStatus().value(status -> assertThat(status)
						.as("공개 경로 %s 가 인증을 요구하면 안 된다", path)
						.isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
	}

	private void expectTokenError(String cookieToken, HttpStatus expectedStatus, TokenErrorCode expectedCode) {
		webTestClient.get()
				.uri(PROTECTED_PATH)
				.cookie(COOKIE_NAME, cookieToken)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.code").isEqualTo(expectedCode.getCode())
				.jsonPath("$.message").isEqualTo(expectedCode.getMessage());
	}

	private static String validToken() {
		return token("1", Duration.ofMinutes(30));
	}

	private static String expiredToken() {
		return token("1", Duration.ofHours(-1));
	}

	private static String token(String subject, Duration expiresIn) {
		return Jwts.builder()
				.subject(subject)
				.claim("role", "USER")
				.issuedAt(Date.from(Instant.now().minus(Duration.ofHours(2))))
				.expiration(Date.from(Instant.now().plus(expiresIn)))
				.signWith(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"), Jwts.SIG.HS256)
				.compact();
	}

}
