package com.spring.careplanservice.careplan.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestSupport {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("todaktodag_test")
            .withUsername("postgres")
            .withPassword("password");

    @Container
    static final RabbitMQContainer RABBIT_MQ = new RabbitMQContainer("rabbitmq:4-alpine");

    // DynamicPropertySource : 테스트하는 동안 Spring RabbitMQ 접속정보를 방금 띄운 컨테이너 주소로 갈아끼워라
    @DynamicPropertySource
    static void registerProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.rabbitmq.host",
                RABBIT_MQ::getHost
        );
        registry.add(
                "spring.rabbitmq.port",
                RABBIT_MQ::getAmqpPort
        );
        registry.add(
                "spring.rabbitmq.username",
                RABBIT_MQ::getAdminUsername
        );
        registry.add(
                "spring.rabbitmq.password",
                RABBIT_MQ::getAdminPassword
        );
    }
}
