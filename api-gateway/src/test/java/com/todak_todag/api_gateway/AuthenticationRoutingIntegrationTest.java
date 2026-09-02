package com.todak_todag.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.store.AccessTokenStore;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * AccessToken Cookie → SHA-256 → 저장소 조회 → JWT 검증 → Spring Security 인증
 * → Gateway 라우팅 → X-User-Id / X-User-Role 전달 까지를 실제 filter chain 으로 확인한다.
 *
 * downstream 은 reactor-netty 로 띄운 테스트 전용 서버이고, lb://user-service 가 그 서버로
 * 해석되도록 discovery 를 대체한다. 라우트 정의 자체는 application.yml 의 운영 설정을 그대로 쓴다.
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
@DisplayName("인증 · 라우팅 통합")
class AuthenticationRoutingIntegrationTest {

	private static final String COOKIE_NAME = "AccessToken";

	private static final String USER_ID_HEADER = "X-User-Id";

	private static final String USER_ROLE_HEADER = "X-User-Role";

	private static final byte[] SECRET_BYTES = Decoders.BASE64.decode(
			"dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA=="
	);

	private static final List<RecordedRequest> DOWNSTREAM_REQUESTS = new CopyOnWriteArrayList<>();

	/**
	 * 컨텍스트가 뜨기 전에 포트가 정해져야 하므로 static 초기화 시점에 띄운다.
	 */
	private static final DisposableServer DOWNSTREAM = HttpServer.create()
			.port(0)
			.handle((request, response) -> request.receive()
					.aggregate()
					.asString()
					.defaultIfEmpty("")
					.flatMap(body -> {
						DOWNSTREAM_REQUESTS.add(RecordedRequest.of(request, body));

						return response.status(HttpResponseStatus.OK)
								.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
								.sendString(Mono.just("{\"downstream\":\"user-service\"}"))
								.then();
					}))
			.bindNow();

	@DynamicPropertySource
	static void routeUserServiceToDownstream(DynamicPropertyRegistry registry) {
		registry.add(
				"spring.cloud.discovery.client.simple.instances.user-service[0].uri",
				() -> "http://localhost:" + DOWNSTREAM.port()
		);
	}

	@AfterAll
	static void stopDownstream() {
		DOWNSTREAM.disposeNow();
	}

	@Autowired
	private Environment environment;

	@MockitoBean
	private AccessTokenStore accessTokenStore;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		DOWNSTREAM_REQUESTS.clear();

		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + environment.getRequiredProperty("local.server.port"))
				.responseTimeout(Duration.ofSeconds(10))
				.build();
	}

	@Test
	@DisplayName("정상 Cookie 요청은 downstream 까지 전달되고 검증된 사용자 헤더가 붙는다")
	void forwardsAuthenticatedRequestWithVerifiedClientHeaders() {
		String token = storedToken();

		webTestClient.get()
				.uri("/api/v1/users/1")
				.cookie(COOKIE_NAME, token)
				.exchange()
				.expectStatus().isOk();

		RecordedRequest forwarded = singleDownstreamRequest();

		assertThat(forwarded.header(USER_ID_HEADER)).isEqualTo("1");
		assertThat(forwarded.header(USER_ROLE_HEADER)).isEqualTo("USER");
	}

	@Test
	@DisplayName("외부에서 보낸 위조 사용자 헤더는 검증된 값으로 교체되어 전달된다")
	void replacesForgedClientHeadersBeforeForwarding() {
		String token = storedToken();

		webTestClient.get()
				.uri("/api/v1/users/1")
				.cookie(COOKIE_NAME, token)
				.header(USER_ID_HEADER, "9999")
				.header(USER_ROLE_HEADER, "ADMIN")
				.exchange()
				.expectStatus().isOk();

		RecordedRequest forwarded = singleDownstreamRequest();

		assertThat(forwarded.headerValues(USER_ID_HEADER)).containsExactly("1");
		assertThat(forwarded.headerValues(USER_ROLE_HEADER)).containsExactly("USER");
	}

	@Test
	@DisplayName("HTTP method, path, query, body 가 그대로 downstream 에 전달된다")
	void preservesMethodPathQueryAndBody() {
		String token = storedToken();

		webTestClient.post()
				.uri("/api/v1/users/1/profile?page=2&size=10")
				.cookie(COOKIE_NAME, token)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"nickname\":\"todak\"}")
				.exchange()
				.expectStatus().isOk();

		RecordedRequest forwarded = singleDownstreamRequest();

		assertThat(forwarded.method()).isEqualTo("POST");
		assertThat(forwarded.uri()).isEqualTo("/api/v1/users/1/profile?page=2&size=10");
		assertThat(forwarded.body()).isEqualTo("{\"nickname\":\"todak\"}");
	}

	@Test
	@DisplayName("Cookie 가 없으면 downstream 을 호출하지 않고 401 이다")
	void doesNotForwardWhenCookieIsAbsent() {
		expectBlocked(
				webTestClient.get().uri("/api/v1/users/1"),
				HttpStatus.UNAUTHORIZED,
				TokenErrorCode.UNAUTHORIZED
		);
	}

	@Test
	@DisplayName("저장소에 토큰이 없으면 downstream 을 호출하지 않고 401 이다")
	void doesNotForwardWhenStoreHasNoToken() {
		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.empty());

		expectBlocked(
				webTestClient.get().uri("/api/v1/users/1").cookie(COOKIE_NAME, validToken()),
				HttpStatus.UNAUTHORIZED,
				TokenErrorCode.INVALID_ACCESS_TOKEN
		);
	}

	@Test
	@DisplayName("만료된 토큰이면 downstream 을 호출하지 않고 401 이다")
	void doesNotForwardWhenTokenIsExpired() {
		String expired = token("1", Duration.ofHours(-1));

		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.just(expired));

		expectBlocked(
				webTestClient.get().uri("/api/v1/users/1").cookie(COOKIE_NAME, expired),
				HttpStatus.UNAUTHORIZED,
				TokenErrorCode.EXPIRED_ACCESS_TOKEN
		);
	}

	@Test
	@DisplayName("저장소 장애면 downstream 을 호출하지 않고 503 이다")
	void doesNotForwardWhenStoreFails() {
		given(accessTokenStore.findByHash(anyString()))
				.willReturn(Mono.error(new RedisConnectionFailureException("redis unavailable")));

		expectBlocked(
				webTestClient.get().uri("/api/v1/users/1").cookie(COOKIE_NAME, validToken()),
				HttpStatus.SERVICE_UNAVAILABLE,
				TokenErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
		);
	}

	@Test
	@DisplayName("같은 이름의 Cookie 가 중복되면 downstream 을 호출하지 않고 401 이다")
	void doesNotForwardWhenCookieIsDuplicated() {
		expectBlocked(
				webTestClient.get()
						.uri("/api/v1/users/1")
						.cookie(COOKIE_NAME, validToken())
						.cookie(COOKIE_NAME, validToken()),
				HttpStatus.UNAUTHORIZED,
				TokenErrorCode.INVALID_ACCESS_TOKEN
		);
	}

	@Test
	@DisplayName("빈 Cookie 는 downstream 을 호출하지 않고 401 이다")
	void doesNotForwardWhenCookieIsBlank() {
		expectBlocked(
				webTestClient.get().uri("/api/v1/users/1").cookie(COOKIE_NAME, ""),
				HttpStatus.UNAUTHORIZED,
				TokenErrorCode.INVALID_ACCESS_TOKEN
		);
	}

	private void expectBlocked(
			WebTestClient.RequestHeadersSpec<?> request,
			HttpStatus expectedStatus,
			TokenErrorCode expectedCode
	) {
		request.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody()
				.jsonPath("$.error.errorCode").isEqualTo(expectedCode.getCode());

		assertThat(DOWNSTREAM_REQUESTS)
				.as("차단된 요청은 downstream 으로 나가면 안 된다")
				.isEmpty();
	}

	/** downstream 은 요청당 정확히 한 번만 호출되어야 한다. */
	private RecordedRequest singleDownstreamRequest() {
		assertThat(DOWNSTREAM_REQUESTS)
				.as("downstream 은 요청당 한 번만 호출되어야 한다")
				.hasSize(1);

		return DOWNSTREAM_REQUESTS.get(0);
	}

	private String storedToken() {
		String token = validToken();

		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.just(token));

		return token;
	}

	private static String validToken() {
		return token("1", Duration.ofMinutes(30));
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

	private record RecordedRequest(
			String method,
			String uri,
			Map<String, List<String>> headers,
			String body
	) {

		private static RecordedRequest of(reactor.netty.http.server.HttpServerRequest request, String body) {
			Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			request.requestHeaders().forEach(entry -> headers
					.computeIfAbsent(entry.getKey(), name -> new ArrayList<>())
					.add(entry.getValue()));

			return new RecordedRequest(
					request.method().name(),
					request.uri(),
					headers,
					body
			);
		}

		private String header(String name) {
			List<String> values = headers.get(name);

			return (values == null || values.isEmpty()) ? null : values.get(0);
		}

		private List<String> headerValues(String name) {
			return headers.getOrDefault(name, List.of());
		}

	}

}
