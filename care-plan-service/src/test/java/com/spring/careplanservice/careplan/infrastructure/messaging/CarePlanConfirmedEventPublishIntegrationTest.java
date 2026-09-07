package com.spring.careplanservice.careplan.infrastructure.messaging;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CarePlanConfirmedEventPublishIntegrationTest {
    @Container
    static final RabbitMQContainer RABBIT_MQ =
            new RabbitMQContainer("rabbitmq:4-alpine");

    // DynamicPropertySource : 테스트하는 동안 Spring RabbitMQ 접속정보를 방금 띄운 컨테이너 주소로 갈아끼워라
    @DynamicPropertySource
    static void rabbitProperties(
            DynamicPropertyRegistry registry
    ) {
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

        /*
        * CONFIRMED 전이 후 실제 RabbitMQ에 이벤트 1건 발행
        payload의 carePlanId, regionId, services, preferences 확인
        다른 상태 변경에서는 Confirmed 이벤트가 발행되지 않는지
        커밋 후 발행되는지
        */
    }
}