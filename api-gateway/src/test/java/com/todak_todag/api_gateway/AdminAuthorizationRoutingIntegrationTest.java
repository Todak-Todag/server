package com.todak_todag.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
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
 * SecurityConfig 의 /api/v1/admin/** role 규칙(MASTER/ADMIN 조합)이
 * 실제 필터 체인 + 라우팅을 통해 의도대로 통과(200)/차단(403) 되는지 확인한다.
 *
 * 인증 메커니즘 자체(쿠키/토큰 유효성)는 AuthenticationRoutingIntegrationTest 에서 검증하므로
 * 여기서는 항상 유효한 토큰을 전제로 하고, role 에 따른 인가 결과만 확인한다.
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
@DisplayName("관리자 경로 인가(Role) 라우팅 통합")
class AdminAuthorizationRoutingIntegrationTest {

	private static final String COOKIE_NAME = "AccessToken";

	private static final byte[] SECRET_BYTES = Decoders.BASE64.decode(
			"dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA=="
	);

	private static final List<String> DOWNSTREAM_HITS = new CopyOnWriteArrayList<>();

	/** 인가 규칙만 확인하면 되므로, 관련 서비스 3개(user/provider/care-plan)를 전부 같은 stub 으로 보낸다. */
	private static final DisposableServer DOWNSTREAM = HttpServer.create()
			.port(0)
			.handle((request, response) -> {
				DOWNSTREAM_HITS.add(request.method().name() + " " + request.uri());

				return response.status(HttpResponseStatus.OK)
						.sendString(Mono.just("{\"downstream\":\"ok\"}"))
						.then();
			})
			.bindNow();

	@DynamicPropertySource
	static void routeAdminServicesToDownstream(DynamicPropertyRegistry registry) {
		String uri = "http://localhost:" + DOWNSTREAM.port();

		registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri", () -> uri);
		registry.add("spring.cloud.discovery.client.simple.instances.provider-service[0].uri", () -> uri);
		registry.add("spring.cloud.discovery.client.simple.instances.care-plan-service[0].uri", () -> uri);
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
		DOWNSTREAM_HITS.clear();

		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + environment.getRequiredProperty("local.server.port"))
				.responseTimeout(Duration.ofSeconds(10))
				.build();
	}

	private record AdminEndpoint(
			HttpMethod method,
			String path,
			Set<String> allowedRoles,
			String deniedRole
	) {
	}

	private static final List<AdminEndpoint> ADMIN_ENDPOINTS = List.of(
			// ===== 그룹 1: MASTER + ADMIN =====
			new AdminEndpoint(
					HttpMethod.PATCH,
					"/api/v1/admin/users/11111111-1111-1111-1111-111111111111/suspend",
					Set.of("MASTER", "ADMIN"),
					"PATIENT"
			),
			new AdminEndpoint(
					HttpMethod.PATCH,
					"/api/v1/admin/users/11111111-1111-1111-1111-111111111111/status",
					Set.of("MASTER", "ADMIN"),
					"PATIENT"
			),
			new AdminEndpoint(
					HttpMethod.GET,
					"/api/v1/admin/users/search",
					Set.of("MASTER", "ADMIN"),
					"PATIENT"
			),
			new AdminEndpoint(
					HttpMethod.GET,
					"/api/v1/admin/service-offerings/regions/22222222-2222-2222-2222-222222222222",
					Set.of("MASTER", "ADMIN"),
					"PATIENT"
			),
			new AdminEndpoint(
					HttpMethod.GET,
					"/api/v1/admin/care-plans",
					Set.of("MASTER", "ADMIN"),
					"PATIENT"
			),

			// ===== 그룹 2: MASTER 전용 (ADMIN 은 반드시 차단되어야 함) =====
			new AdminEndpoint(HttpMethod.POST, "/api/v1/admin/users", Set.of("MASTER"), "ADMIN"),
			new AdminEndpoint(HttpMethod.POST, "/api/v1/admin/regions", Set.of("MASTER"), "ADMIN"),
			new AdminEndpoint(HttpMethod.GET, "/api/v1/admin/regions", Set.of("MASTER"), "ADMIN"),
			new AdminEndpoint(
					HttpMethod.PATCH,
					"/api/v1/admin/regions/33333333-3333-3333-3333-333333333333/status",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(
					HttpMethod.PATCH,
					"/api/v1/admin/regions/33333333-3333-3333-3333-333333333333",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(
					HttpMethod.DELETE,
					"/api/v1/admin/regions/33333333-3333-3333-3333-333333333333",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(HttpMethod.POST, "/api/v1/admin/consent-documents", Set.of("MASTER"), "ADMIN"),
			new AdminEndpoint(
					HttpMethod.POST,
					"/api/v1/admin/consent-documents/44444444-4444-4444-4444-444444444444/versions",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(
					HttpMethod.PATCH,
					"/api/v1/admin/consent-documents/44444444-4444-4444-4444-444444444444/required",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(
					HttpMethod.DELETE,
					"/api/v1/admin/consent-documents/44444444-4444-4444-4444-444444444444",
					Set.of("MASTER"),
					"ADMIN"
			),
			new AdminEndpoint(HttpMethod.POST, "/api/v1/admin/provide-services", Set.of("MASTER"), "ADMIN")
	);

	static Stream<Arguments> allowedRoleCases() {
		return ADMIN_ENDPOINTS.stream()
				.flatMap(endpoint -> endpoint.allowedRoles().stream()
						.map(role -> Arguments.of(endpoint.method(), endpoint.path(), role)));
	}

	static Stream<Arguments> deniedRoleCases() {
		return ADMIN_ENDPOINTS.stream()
				.map(endpoint -> Arguments.of(endpoint.method(), endpoint.path(), endpoint.deniedRole()));
	}

	@ParameterizedTest(name = "{0} {1} - {2} 는 통과되어 downstream 까지 도달한다")
	@MethodSource("allowedRoleCases")
	@DisplayName("허용된 role은 200으로 downstream 까지 도달한다")
	void allowedRoleReachesDownstream(HttpMethod method, String path, String role) {
		String token = storedToken(role);

		webTestClient.method(method)
				.uri(path)
				.cookie(COOKIE_NAME, token)
				.exchange()
				.expectStatus().isOk();

		assertThat(DOWNSTREAM_HITS).hasSize(1);
	}

	@ParameterizedTest(name = "{0} {1} - {2} 는 403으로 차단되고 downstream 을 호출하지 않는다")
	@MethodSource("deniedRoleCases")
	@DisplayName("허용되지 않은 role은 403으로 차단된다")
	void deniedRoleIsForbidden(HttpMethod method, String path, String deniedRole) {
		String token = storedToken(deniedRole);

		webTestClient.method(method)
				.uri(path)
				.cookie(COOKIE_NAME, token)
				.exchange()
				.expectStatus().isForbidden()
				.expectBody()
				.jsonPath("$.error.errorCode").isEqualTo(TokenErrorCode.ACCESS_DENIED.getCode());

		assertThat(DOWNSTREAM_HITS).isEmpty();
	}

	private String storedToken(String role) {
		String token = token(role);

		given(accessTokenStore.findByHash(anyString())).willReturn(Mono.just(token));

		return token;
	}

	private static String token(String role) {
		return Jwts.builder()
				.subject("99999999-9999-9999-9999-999999999999")
				.claim("role", role)
				.issuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(1))))
				.expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
				.signWith(new SecretKeySpec(SECRET_BYTES, "HmacSHA512"), Jwts.SIG.HS512)
				.compact();
	}
}
