package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 수신 이벤트 처리가 계속 실패할 때 재시도를 소진하고 DLQ로 옮겨지는지 검증
// 실패는 Mock이 아니라 실제 도메인 규칙 위반으로 만듦
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderMatchingEventDlqIntegrationTest extends PostgresTestSupport {

    // 최초 1회 + 재시도(1초 간격)를 넉넉히 기다림
    private static final long DEAD_LETTER_TIMEOUT_MS = 20_000;

    private static final String FAILURE_REASON = "해당 지역에 가능한 서비스 제공자가 없습니다.";

    @Container
    static final RabbitMQContainer RABBIT_MQ = new RabbitMQContainer("rabbitmq:4-alpine");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT_MQ::getHost);
        registry.add("spring.rabbitmq.port", RABBIT_MQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT_MQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT_MQ::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @BeforeEach
    void clear() {
        springDataServiceMatchingAttemptRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();

        amqpAdmin.purgeQueue(RabbitMqConfig.SCHEDULE_PROVIDER_MATCHED_DLQ, false);
        amqpAdmin.purgeQueue(RabbitMqConfig.SCHEDULE_PROVIDER_MATCH_FAILED_DLQ, false);
    }

    @Test
    @DisplayName("ProviderMatched 처리가 계속 실패하면 재시도 소진 후 원래 큐가 아닌 DLQ로 옮겨진다")
    void 매칭_이벤트_재시도_소진시_DLQ로_이동한다() {
        // given
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        // when
        publishMatched(UUID.randomUUID(), servicePreferenceId, UUID.randomUUID(), today);

        // then
        Message deadLetter = rabbitTemplate.receive(
                RabbitMqConfig.SCHEDULE_PROVIDER_MATCHED_DLQ, DEAD_LETTER_TIMEOUT_MS
        );

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8))
                .contains(servicePreferenceId.toString());

        assertThat(rabbitTemplate.receive(RabbitMqConfig.SCHEDULE_PROVIDER_MATCHED_QUEUE, 1000)).isNull();

        assertThat(springDataServiceScheduleRepository.findAll()).isEmpty();
        assertThat(springDataServiceMatchingAttemptRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("ProviderMatchFailed 처리가 계속 실패하면 재시도 소진 후 원래 큐가 아닌 DLQ로 옮겨진다")
    void 매칭실패_이벤트_재시도_소진시_DLQ로_이동한다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        saveRescheduling(carePlanId, servicePreferenceId, date);
        saveRescheduling(carePlanId, servicePreferenceId, date.plusDays(1));

        // when
        publishMatchFailed(carePlanId, servicePreferenceId, date.plusDays(2));

        // then
        Message deadLetter = rabbitTemplate.receive(
                RabbitMqConfig.SCHEDULE_PROVIDER_MATCH_FAILED_DLQ, DEAD_LETTER_TIMEOUT_MS
        );

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8))
                .contains(servicePreferenceId.toString());

        assertThat(rabbitTemplate.receive(RabbitMqConfig.SCHEDULE_PROVIDER_MATCH_FAILED_QUEUE, 1000)).isNull();

        List<ServiceSchedule> schedules = springDataServiceScheduleRepository.findAll();
        assertThat(schedules).hasSize(2);
        assertThat(schedules).allMatch(schedule -> schedule.getStatus() == ScheduleStatus.RESCHEDULING);
        assertThat(springDataServiceMatchingAttemptRepository.findAll()).isEmpty();
    }

    private void publishMatched(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date
    ) {
        String json = """
                {
                  "carePlanId": "%s",
                  "regionId": "%s",
                  "provideServiceId": "%s",
                  "servicePreferenceId": "%s",
                  "serviceOfferingId": "%s",
                  "date": "%s",
                  "startedAt": "%sT10:00:00",
                  "matchedAt": "2026-08-29T10:00:00Z"
                }
                """.formatted(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                serviceOfferingId,
                date,
                date
        );

        send(RabbitMqConfig.PROVIDER_MATCHED_ROUTING_KEY, json);
    }

    private void publishMatchFailed(UUID carePlanId, UUID servicePreferenceId, LocalDate date) {
        String json = """
                {
                  "carePlanId": "%s",
                  "regionId": "%s",
                  "provideServiceId": "%s",
                  "servicePreferenceId": "%s",
                  "date": "%s",
                  "preferredTimeSlot": "MORNING",
                  "failureReason": "%s",
                  "failedAt": "2026-08-29T10:00:00Z"
                }
                """.formatted(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                date,
                FAILURE_REASON
        );

        send(RabbitMqConfig.PROVIDER_MATCH_FAILED_ROUTING_KEY, json);
    }

    private void send(String routingKey, String json) {
        Message message = MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .andProperties(MessagePropertiesBuilder.newInstance()
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build())
                .build();

        rabbitTemplate.send(RabbitMqConfig.PROVIDER_EXCHANGE, routingKey, message);
    }

    // RESCHEDULING 상태의 기존 일정을 저장
    private void saveRescheduling(UUID carePlanId, UUID servicePreferenceId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );

        schedule.rescheduling();

        springDataServiceScheduleRepository.save(schedule);
    }
}
