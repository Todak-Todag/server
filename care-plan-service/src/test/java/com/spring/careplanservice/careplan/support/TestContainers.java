package com.spring.careplanservice.careplan.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

public final class TestContainers {
    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("todaktodag_test")
                    .withUsername("postgres")
                    .withPassword("password");

    public static final RabbitMQContainer RABBIT_MQ =
            new RabbitMQContainer("rabbitmq:4-alpine");

    static {
        POSTGRES.start();
        RABBIT_MQ.start();
    }

    private TestContainers() {
    }
}
