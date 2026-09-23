package com.todak_todag.provider_service.provider.infrastructure.messaging;

import com.todak_todag.provider_service.global.config.RabbitMqConfig;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.facade.OutboxRelayFacade;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaOutboxEventRepository;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아웃박스 릴레이 통합")
class OutboxRelayIntegrationTest extends ContainerTestSupport {

    // provider.exchange 에서 매칭 결과를 받아볼 테스트 전용 큐
    private static final String TEST_QUEUE = "test.provider-matched.queue";

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);
    private static final Instant FIXED_TIME = Instant.parse("2026-09-07T09:00:00Z");

    @Autowired
    private MatchingEventPort matchingEventPort;

    @Autowired
    private OutboxRelayFacade outboxRelayFacade;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID servicePreferenceId;

    @BeforeEach
    void setUp() {
        servicePreferenceId = UUID.randomUUID();

        jpaOutboxEventRepository.deleteAll();
        declareTestQueue();
    }

    @Test
    @DisplayName("매칭 결과는 브로커로 바로 가지 않고 아웃박스에 미발행 상태로 쌓인다")
    void publishMatched_appendsPending() {
        matchingEventPort.publishMatched(matchedEvent());

        List<ProviderOutboxEvent> pending = jpaOutboxEventRepository
                .findAllByPublishedAtIsNullAndRetryCountLessThanOrderByCreatedAtAsc(
                        ProviderOutboxEvent.MAX_RETRY_COUNT, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getAggregateId()).isEqualTo(servicePreferenceId);
        assertThat(pending.get(0).getPublishedAt()).isNull();

        // 릴레이를 돌리기 전이므로 브로커에는 아무것도 없다
        assertThat(rabbitTemplate.receive(TEST_QUEUE, 500)).isNull();
    }

    @Test
    @DisplayName("릴레이가 돌면 브로커로 발행되고 발행 시각이 기록된다")
    void relay_publishesAndMarks() {
        ProviderMatchedEvent event = matchedEvent();
        matchingEventPort.publishMatched(event);

        outboxRelayFacade.relay();

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5000);
        assertThat(message).isNotNull();

        // 수신 측 서비스가 실제로 보게 될 JSON 을 그대로 검증한다
        JsonNode payload = objectMapper.readTree(message.getBody());
        assertThat(payload.get("servicePreferenceId").asText()).isEqualTo(servicePreferenceId.toString());
        assertThat(payload.get("date").asText()).isEqualTo("2026-09-14");

        List<ProviderOutboxEvent> all = jpaOutboxEventRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getPublishedAt()).isNotNull();
        assertThat(all.get(0).getRetryCount()).isZero();
    }

    @Test
    @DisplayName("이미 발행된 건은 릴레이가 다시 집어가지 않는다")
    void relay_skipsPublished() {
        matchingEventPort.publishMatched(matchedEvent());

        outboxRelayFacade.relay();
        rabbitTemplate.receive(TEST_QUEUE, 5000);

        outboxRelayFacade.relay();

        assertThat(rabbitTemplate.receive(TEST_QUEUE, 500)).isNull();
    }

    @Test
    @DisplayName("재시도 한도에 닿은 건은 릴레이가 더 이상 집어가지 않고 삭제하지도 않는다")
    void relay_skipsRetryExhausted() {
        matchingEventPort.publishMatched(matchedEvent());
        failStored(ProviderOutboxEvent.MAX_RETRY_COUNT);

        outboxRelayFacade.relay();

        assertThat(rabbitTemplate.receive(TEST_QUEUE, 500)).isNull();

        // 원인 확인과 수동 재처리를 위해 미발행 상태로 남아 있어야 한다
        List<ProviderOutboxEvent> all = jpaOutboxEventRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getPublishedAt()).isNull();
        assertThat(all.get(0).getRetryCount()).isEqualTo(ProviderOutboxEvent.MAX_RETRY_COUNT);
    }

    @Test
    @DisplayName("재시도 한도 직전인 건은 여전히 발행된다")
    void relay_publishesBelowMax() {
        matchingEventPort.publishMatched(matchedEvent());
        failStored(ProviderOutboxEvent.MAX_RETRY_COUNT - 1);

        outboxRelayFacade.relay();

        assertThat(rabbitTemplate.receive(TEST_QUEUE, 5000)).isNotNull();
        assertThat(jpaOutboxEventRepository.findAll().get(0).getPublishedAt()).isNotNull();
    }

    // 저장된 아웃박스 이벤트를 지정한 횟수만큼 실패한 상태로 만든다
    private void failStored(int count) {
        ProviderOutboxEvent event = jpaOutboxEventRepository.findAll().get(0);
        for (int i = 0; i < count; i++) {
            event.recordFailure("발행 실패");
        }
        jpaOutboxEventRepository.save(event);
    }

    private ProviderMatchedEvent matchedEvent() {
        return new ProviderMatchedEvent(
                UUID.randomUUID(), UUID.randomUUID(), servicePreferenceId, UUID.randomUUID(),
                UUID.randomUUID(), MONDAY, MONDAY.atTime(9, 0), FIXED_TIME
        );
    }

    // provider.exchange 는 애플리케이션이 선언하지만 바인딩된 큐는 없다
    // 발행 결과를 확인하려면 테스트가 직접 큐를 붙여야 한다
    private void declareTestQueue() {
        // durable=true : RabbitMQ 4부터 비내구성 비배타 큐(transient_nonexcl_queues)를 선언할 수 없다
        // autoDelete=false : receive()가 컨슈머를 붙였다 떼면서 autoDelete 큐를 지워버린다
        // 컨테이너가 테스트 종료와 함께 사라지므로 큐를 남겨도 문제되지 않는다
        Queue queue = new Queue(TEST_QUEUE, true, false, false);
        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue)
                .to(new DirectExchange(RabbitMqConfig.PROVIDER_EXCHANGE))
                .with(RabbitMqConfig.PROVIDER_MATCHED_KEY);

        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(TEST_QUEUE);
    }
}