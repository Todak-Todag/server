package com.todak_todag.provider_service.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

// 컨테이너를 static 블록에서 직접 띄워 JVM 전체에서 한 번만 기동한다
// @Testcontainers + @Container 를 쓰면 클래스마다 컨테이너가 재시작되는데,
// Spring 컨텍스트는 캐시되어 재사용되므로 두 번째 클래스가 죽은 포트에 붙는다
@SpringBootTest(properties = {
        // 스케줄러가 임의 시점에 발행하면 검증 시점을 통제할 수 없다
        // 테스트는 OutboxRelayFacade.relay() 를 직접 호출한다
        "provider.outbox.relay.enabled=false",

        // 운영과 같이 초기화 스크립트가 만든 스키마를 검증만 한다
        // 스크립트와 엔티티가 어긋나면 컨텍스트 기동이 실패해 배포 전에 잡힌다
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true"
})
public abstract class ContainerTestSupport {

    // docker/postgres/provider-service.sql 을 그대로 실행한다
    // 원본은 build.gradle의 processTestResources가 테스트 리소스로 복사한다
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("todaktodag_test")
                    .withInitScript("db/provider-service.sql");

    static final RabbitMQContainer RABBIT_MQ =
            new RabbitMQContainer("rabbitmq:4-alpine");

    static {
        POSTGRES.start();
        RABBIT_MQ.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", RABBIT_MQ::getHost);
        registry.add("spring.rabbitmq.port", RABBIT_MQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT_MQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT_MQ::getAdminPassword);
    }
}