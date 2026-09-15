package com.todak_todag.provider_service.provider.infrastructure.messaging;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.global.config.RabbitMqConfig;
import com.todak_todag.provider_service.provider.application.event.CarePlanConfirmedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderRematchedEvent;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaOutboxEventRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 다른 컨테이너 통합 테스트와 컨텍스트를 공유해야 리스너가 하나만 떠서 메시지를 나눠 받지 않는다
// 그래서 Mock 대신 실제 빈으로 실패를 만든다
// 후보 제공 서비스가 있으면 점유 조회로 Schedule-Service를 호출하는데, 테스트에는 인스턴스가 없어 503(재시도 대상)이 계속 난다
@DisplayName("매칭 메시지 재시도 소진 DLQ 통합")
class MatchingMessageDlqIntegrationTest extends ContainerTestSupport {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    // 최초 1회 + 재시도 3회(1·2·4초 대기)를 넉넉히 기다린다
    private static final long DEAD_LETTER_TIMEOUT_MS = 20_000;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JpaServiceOfferingRepository jpaServiceOfferingRepository;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    private final UUID regionId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        amqpAdmin.purgeQueue(RabbitMqConfig.CARE_PLAN_CONFIRMED_DLQ, false);
        amqpAdmin.purgeQueue(RabbitMqConfig.SCHEDULE_REMATCHED_DLQ, false);

        jpaServiceOfferingRepository.saveAndFlush(ServiceOffering.of(UUID.randomUUID(), provideServiceId, regionId));
    }

    @Test
    @DisplayName("CarePlanConfirmed 처리가 계속 실패하면 재시도 소진 후 원래 큐가 아닌 DLQ로 옮겨진다")
    void carePlanConfirmed_retryExhausted_deadLetters() {
        UUID preferenceId = UUID.randomUUID();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CARE_PLAN_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_CONFIRMED_KEY,
                new CarePlanConfirmedEvent(UUID.randomUUID(), regionId, List.of(
                        new CarePlanConfirmedEvent.Service(UUID.randomUUID(), provideServiceId, List.of(
                                new CarePlanConfirmedEvent.Preference(preferenceId, MONDAY, TimeSlot.MORNING)
                        ))
                ))
        );

        Message deadLetter = rabbitTemplate.receive(RabbitMqConfig.CARE_PLAN_CONFIRMED_DLQ, DEAD_LETTER_TIMEOUT_MS);

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).contains(preferenceId.toString());
        assertThat(rabbitTemplate.receive(RabbitMqConfig.CARE_PLAN_CONFIRMED_QUEUE, 1000)).isNull();
        assertThat(jpaOutboxEventRepository.findAll())
                .noneMatch(outboxEvent -> outboxEvent.getAggregateId().equals(preferenceId));
    }

    @Test
    @DisplayName("재매칭 처리가 계속 실패하면 재시도 소진 후 원래 큐가 아닌 DLQ로 옮겨진다")
    void rematch_retryExhausted_deadLetters() {
        UUID preferenceId = UUID.randomUUID();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCHEDULE_EXCHANGE,
                RabbitMqConfig.SCHEDULE_REMATCHED_KEY,
                new ProviderRematchedEvent(
                        UUID.randomUUID(), regionId, provideServiceId, preferenceId, MONDAY, TimeSlot.MORNING
                )
        );

        Message deadLetter = rabbitTemplate.receive(RabbitMqConfig.SCHEDULE_REMATCHED_DLQ, DEAD_LETTER_TIMEOUT_MS);

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).contains(preferenceId.toString());
        assertThat(rabbitTemplate.receive(RabbitMqConfig.SCHEDULE_REMATCHED_QUEUE, 1000)).isNull();
    }
}