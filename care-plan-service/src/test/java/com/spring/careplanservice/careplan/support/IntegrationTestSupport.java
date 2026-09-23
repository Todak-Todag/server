package com.spring.careplanservice.careplan.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                TestContainers.POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                TestContainers.POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                TestContainers.POSTGRES::getPassword
        );

        registry.add(
                "spring.rabbitmq.host",
                TestContainers.RABBIT_MQ::getHost
        );
        registry.add(
                "spring.rabbitmq.port",
                TestContainers.RABBIT_MQ::getAmqpPort
        );
        registry.add(
                "spring.rabbitmq.username",
                TestContainers.RABBIT_MQ::getAdminUsername
        );
        registry.add(
                "spring.rabbitmq.password",
                TestContainers.RABBIT_MQ::getAdminPassword
        );
    }
}
