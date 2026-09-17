package com.todak_todag.user_service.user.presentation.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;
import com.todak_todag.user_service.user.presentation.request.UserLoginRequest;

// 클래스 레벨 @Transactional 을 일부러 두지 않는다 — 그걸 걸면 테스트 스레드가 만든 데이터가
// 커밋되지 않아, 별도 스레드에서 실행되는 로그인 요청이 그 데이터를 보지 못한다.
// 진짜 동시 요청을 재현하려면 각 요청이 독립된 트랜잭션/커넥션으로 실행돼야 한다.
//
// 테스트 프로필 기본값(ddl-auto: create-drop, flyway: false)은 엔티티 애노테이션만으로
// 스키마를 만들어서, V1__init.sql에만 있는 부분 유니크 인덱스(ux_p_auths_user_active)가
// 아예 생기지 않는다 — 그러면 이 테스트가 검증하려는 방어선 자체가 없는 스키마로 돌게 된다.
// 그래서 Flyway를 켜서 실제 프로덕션과 같은 스키마로 검증하는데, PostgresTestSupport의
// static 컨테이너는 JVM 전체(다른 통합테스트 클래스 전부)가 공유하고 스위트가 끝날 때까지
// 안 지워져서, 그 컨테이너에 Flyway를 얹으면 다른 테스트가 create-drop 으로 미리 만들어둔
// 스키마와 충돌한다(Flyway는 flyway_schema_history 없이 비어있지 않은 스키마를 거부).
// 그래서 이 클래스만 별도의 전용 컨테이너를 띄운다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.flyway.enabled=true",
		"spring.flyway.schemas=user_schema",
		"spring.flyway.default-schema=user_schema",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("로그인 동시성 통합테스트")
class AuthLoginConcurrencyIntegrationTest {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

	private static final GenericContainer<?> REDIS =
			new GenericContainer<>(DockerImageName.parse("redis:7.4")).withExposedPorts(6379);

	static {
		POSTGRES.start();
		REDIS.start();
	}

	@DynamicPropertySource
	static void containerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	private static final String RAW_PASSWORD = "Test1234!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JpaUserRepository jpaUserRepository;

	@Autowired
	private JpaAuthRepository jpaAuthRepository;

	@Autowired
	private PasswordEncoderPort passwordEncoder;

	private User testUser;

	@AfterEach
	void cleanUp() {
		// 클래스에 @Transactional 이 없어 각 테스트가 만든 데이터가 실제로 커밋되므로 직접 지운다.
		if (testUser != null) {
			jpaAuthRepository.findAll().stream()
					.filter(auth -> auth.getUserId().equals(testUser.getId()))
					.forEach(jpaAuthRepository::delete);
			jpaUserRepository.deleteById(testUser.getId());
		}
	}

	@Test
	@DisplayName("같은 유저가 동시에 로그인해도 전부 성공하고, 활성 세션은 정확히 1개만 남는다")
	void concurrentLogin_allSucceed_andExactlyOneActiveSessionRemains() throws Exception {
		String username = "concurrentlogin1";
		testUser = jpaUserRepository.save(User.createAdmin(
				UUID.randomUUID(), username, passwordEncoder.encode(RAW_PASSWORD), "동시성테스트", "01012345670"
		));

		int concurrency = 8;
		ExecutorService executor = Executors.newFixedThreadPool(concurrency);
		CountDownLatch ready = new CountDownLatch(concurrency);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		try {
			String body = objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD));

			for (int i = 0; i < concurrency; i++) {
				executor.submit(() -> {
					try {
						// 모든 스레드가 이 지점에 도착할 때까지 기다렸다가, 동시에 요청을 쏜다.
						ready.countDown();
						start.await();

						int status = mockMvc.perform(post("/api/v1/auth/login")
										.contentType(MediaType.APPLICATION_JSON)
										.content(body))
								.andReturn()
								.getResponse()
								.getStatus();

						if (status == 204) {
							successCount.incrementAndGet();
						} else {
							failureCount.incrementAndGet();
						}
					} catch (Exception e) {
						failureCount.incrementAndGet();
					}
				});
			}

			ready.await(5, TimeUnit.SECONDS);
			start.countDown();
			executor.shutdown();
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		// 유니크 제약(ux_p_auths_user_active) 위반이 잡히지 않으면 일부가 500으로 실패한다.
		assertThat(failureCount.get())
				.as("동시 로그인 요청 중 실패(500 등)한 건수")
				.isZero();
		assertThat(successCount.get()).isEqualTo(concurrency);

		List<Auth> activeSessions = jpaAuthRepository.findAll().stream()
				.filter(auth -> auth.getUserId().equals(testUser.getId()))
				.filter(auth -> auth.getLogoutAt() == null)
				.toList();

		assertThat(activeSessions)
				.as("동시 로그인 후에도 활성 세션은 정확히 1개여야 한다")
				.hasSize(1);
	}
}
