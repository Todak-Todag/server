package com.todak_todag.provider_service.provider.infrastructure.messaging;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.global.config.RabbitMqConfig;
import com.todak_todag.provider_service.provider.application.event.CarePlanConfirmedEvent;
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaOutboxEventRepository;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("CarePlanConfirmed 수신 통합")
class CarePlanConfirmedConsumeIntegrationTest extends ContainerTestSupport {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    private UUID servicePreferenceId;

    @BeforeEach
    void setUp() {
        servicePreferenceId = UUID.randomUUID();

        jpaOutboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("제공자가 없으면 매칭 실패 이벤트가 아웃박스에 적재된다")
    void consume_noProvider_appendsMatchFailed() {
        // 후보가 없는 지역·서비스 종류라 매칭은 반드시 실패한다
        CarePlanConfirmedEvent event = new CarePlanConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new CarePlanConfirmedEvent.Service(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(new CarePlanConfirmedEvent.Preference(
                                servicePreferenceId, MONDAY, TimeSlot.MORNING
                        ))
                ))
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CARE_PLAN_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_CONFIRMED_KEY,
                event
        );

        // 리스너가 비동기로 처리하므로 적재될 때까지 기다린다
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<ProviderOutboxEvent> all = jpaOutboxEventRepository.findAll();

            assertThat(all).hasSize(1);
            assertThat(all.get(0).getEventType()).isEqualTo(OutboxEventType.PROVIDER_MATCH_FAILED);
            assertThat(all.get(0).getAggregateId()).isEqualTo(servicePreferenceId);
            assertThat(all.get(0).getPublishedAt()).isNull();
        });
    }
}