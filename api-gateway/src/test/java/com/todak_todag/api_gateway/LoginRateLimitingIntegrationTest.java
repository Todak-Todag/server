package com.todak_todag.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.lettuce.core.api.StatefulRedisConnection;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * /api/v1/auth/login 에 붙인 RequestRateLimiter 필터(Bucket4jRateLimiter)가
 * 실제 요청 경로에서 한도를 지키는지, 한도를 넘긴 요청은 downstream 까지 안 가는지,
 * 다른 라우트에는 영향이 없는지를 확인한다.
 *
 * 테스트 전용으로 버킷을 아주 작게(capacity=3, refill-period=2s) 오버라이드해서
 * 실제 운영값(20/1m)을 기다리지 않고도 빠르게 검증한다.
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"jwt.secret=dG9kYWstdG9kYWctYXBpLWdhdGV3YXktdGVzdC1qd3Qtc2VjcmV0LWtleS1oczI1Ni0wMTIzNDU2Nzg5YWJjZA==",
				"internal.key=test-internal-key",
				"eureka.client.enabled=false",
				"eureka.client.register-with-eureka=false",
				"eureka.client.fetch-registry=false",
				"management.tracing.enabled=false",
				"rate-limit.buckets.user-service-login.capacity=3",
				"rate-limit.buckets.user-service-login.refill-tokens=3",
				"rate-limit.buckets.user-service-login.refill-period=2s"
		}
)
@DisplayName("로그인 레이트리밋 통합")
class LoginRateLimitingIntegrationTest {

	private static final int CAPACITY = 3;

	private static final AtomicInteger DOWNSTREAM_CALL_COUNT = new AtomicInteger();

	/**
	 * 컨텍스트가 뜨기 전에 포트가 정해져야 하므로 static 초기화 시점에 띄운다.
	 * 요청이 어떤 경로로 오든 항상 200 을 준다 - 여기서 관심사는 로그인 성공 여부가 아니라
	 * "레이트리밋을 통과한 요청 수" 이기 때문이다.
	 */
	private static final DisposableServer DOWNSTREAM = HttpServer.create()
			.port(0)
			.handle((request, response) -> {
				DOWNSTREAM_CALL_COUNT.incrementAndGet();

				return response.status(HttpResponseStatus.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.sendString(Mono.just("{\"downstream\":\"user-service\"}"))
						.then();
			})
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

	@Autowired
	private StatefulRedisConnection<String, byte[]> rateLimitRedisConnection;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		DOWNSTREAM_CALL_COUNT.set(0);

		// 이전 테스트 메서드가 소진한 버킷 상태가 남아있으면 이번 테스트가 오염된다.
		// 라우트 id 를 접두사로 쓰므로, 이 라우트에 해당하는 키만 골라 지운다.
		List<String> staleKeys = rateLimitRedisConnection.sync().keys("user-service-login:*");

		if (!staleKeys.isEmpty()) {
			rateLimitRedisConnection.sync().del(staleKeys.toArray(new String[0]));
		}

		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + environment.getRequiredProperty("local.server.port"))
				.responseTimeout(Duration.ofSeconds(10))
				.build();
	}

	@Test
	@DisplayName("한도 이내 요청은 모두 downstream 까지 통과한다")
	void success_withinCapacityPassesThrough() {
		for (int i = 0; i < CAPACITY; i++) {
			postLogin().expectStatus().isOk();
		}

		assertThat(DOWNSTREAM_CALL_COUNT.get()).isEqualTo(CAPACITY);
	}

	@Test
	@DisplayName("한도를 넘긴 요청은 429 이고, downstream 은 호출되지 않는다")
	void blocksRequestBeyondCapacityWithoutCallingDownstream() {
		for (int i = 0; i < CAPACITY; i++) {
			postLogin().expectStatus().isOk();
		}

		postLogin().expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

		assertThat(DOWNSTREAM_CALL_COUNT.get())
				.as("한도를 넘긴 마지막 요청은 downstream 으로 나가면 안 된다")
				.isEqualTo(CAPACITY);
	}

	@Test
	@DisplayName("리필 주기가 지나면 다시 요청이 통과한다")
	void allowsRequestAgainAfterRefillPeriodElapses() throws InterruptedException {
		for (int i = 0; i < CAPACITY; i++) {
			postLogin().expectStatus().isOk();
		}

		postLogin().expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

		// refill-period=2s 보다 넉넉히 기다린다.
		Thread.sleep(2_200);

		postLogin().expectStatus().isOk();
	}

	@Test
	@DisplayName("로그인 라우트가 아닌 다른 라우트는 레이트리밋의 영향을 받지 않는다")
	void doesNotAffectOtherRoutes() {
		for (int i = 0; i < CAPACITY + 2; i++) {
			webTestClient.get()
					.uri("/api/v1/regions/1")
					.exchange()
					.expectStatus().isOk();
		}

		assertThat(DOWNSTREAM_CALL_COUNT.get()).isEqualTo(CAPACITY + 2);
	}

	private WebTestClient.ResponseSpec postLogin() {
		return webTestClient.post()
				.uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"username\":\"whoever\",\"password\":\"whatever\"}")
				.exchange();
	}

}
