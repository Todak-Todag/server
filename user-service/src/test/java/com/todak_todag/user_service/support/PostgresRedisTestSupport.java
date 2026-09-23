package com.todak_todag.user_service.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

// PostgreSQL에 더해 실제 Redis도 필요한 통합 테스트의 공통 베이스 클래스
public abstract class PostgresRedisTestSupport extends PostgresTestSupport {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    // Testcontainers가 생성한 Redis 연결 정보를 테스트 환경에 주입
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
