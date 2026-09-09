package com.spring.careplanservice.careplan.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import com.spring.careplanservice.careplan.support.IntegrationTestSupport;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CarePlanCompletedEventPublishIntegrationTest extends IntegrationTestSupport {

    private static final String TEST_QUEUE = "test.social-worker.care-plan-completed.queue";

    @Autowired
    private CarePlanCompletedEventPort carePlanCompletedEventPort;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CarePlanCompleted 이벤트를 지정된 Exchange와 Routing Key로 발행")
    void publishCarePlanCompletedEvent_success() throws Exception {
        Queue queue = QueueBuilder
                .nonDurable(TEST_QUEUE)
                .exclusive()
                .autoDelete()
                .build();

        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder
                .bind(queue)
                .to(new org.springframework.amqp.core.DirectExchange(RabbitMqConfig.CARE_PLAN_COMPLETED_EXCHANGE))
                .with(RabbitMqConfig.CARE_PLAN_COMPLETED_ROUTING_KEY);

        amqpAdmin.declareBinding(binding);

        UUID eventId = UUID.randomUUID();
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Instant completedAt = Instant.parse(
                "2026-09-08T15:00:00Z"
        );

        CarePlanCompletionEvent event = new CarePlanCompletionEvent(
                eventId,
                carePlanId,
                patientId,
                completedAt
        );

        carePlanCompletedEventPort.publish(event);

        org.springframework.amqp.core.Message message = rabbitTemplate.receive(
                TEST_QUEUE,
                5000
        );

        assertThat(message).isNotNull();

        JsonNode payload = objectMapper.readTree(
                message.getBody()
        );

        assertThat(payload.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(payload.get("carePlanId").asText()).isEqualTo(carePlanId.toString());
        assertThat(payload.get("patientId").asText()).isEqualTo(patientId.toString());
        assertThat(payload.get("completedAt").asText()).isEqualTo("2026-09-08T15:00:00Z");
    }
}
