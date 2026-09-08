package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

// ProviderMatchFailed 수신 통합 테스트
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderMatchFailedEventConsumeIntegrationTest extends PostgresTestSupport {

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
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @BeforeEach
    void clear() {
        springDataServiceMatchingAttemptRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("재매칭 실패 이벤트를 수신하면 기존 RESCHEDULING 일정이 SCHEDULED로 복구된다")
    void 재매칭_실패로_일정이_복구된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate originalDate = LocalDate.now().plusDays(5);

        ServiceSchedule existing = saveRescheduling(carePlanId, servicePreferenceId, originalDate);

        // when
        publish(carePlanId, servicePreferenceId, originalDate.plusDays(1), "2026-08-29T10:00:00Z");

        // then
        await(() -> springDataServiceScheduleRepository.findById(existing.getId())
                .map(schedule -> schedule.getStatus() == ScheduleStatus.SCHEDULED)
                .orElse(false));

        ServiceSchedule restored = springDataServiceScheduleRepository.findById(existing.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(restored.getDate()).isEqualTo(originalDate);
        assertThat(restored.getServiceOfferingId()).isEqualTo(existing.getServiceOfferingId());

        assertThat(restored.getCancelReason()).isNull();
        assertThat(restored.getCanceledAt()).isNull();

        assertThat(springDataServiceScheduleRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("초기 매칭 실패 이벤트를 수신하면 일정은 만들지 않고 매칭 이력만 남는다")
    void 초기_매칭_실패는_이력만_남긴다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        // when
        publish(carePlanId, servicePreferenceId, date, "2026-08-29T10:00:00Z");

        // then
        await(() -> !springDataServiceMatchingAttemptRepository.findAll().isEmpty());

        assertThat(springDataServiceScheduleRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("수신한 매칭 실패 결과가 p_service_matching_attempts에 기록된다")
    void 매칭_실패_결과가_기록된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        // when
        publish(carePlanId, servicePreferenceId, date, "2026-08-29T10:00:00Z");

        // then
        await(() -> !springDataServiceMatchingAttemptRepository.findAll().isEmpty());

        List<ServiceMatchingAttempt> attempts = springDataServiceMatchingAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);

        ServiceMatchingAttempt attempt = attempts.getFirst();
        assertThat(attempt.getCarePlanId()).isEqualTo(carePlanId);
        assertThat(attempt.getServicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(attempt.getDate()).isEqualTo(date);
        assertThat(attempt.getStatus()).isEqualTo(MatchingAttemptStatus.FAILED);
        assertThat(attempt.getFailureReason()).isEqualTo(FAILURE_REASON);
        assertThat(attempt.getFailedAt()).isEqualTo(Instant.parse("2026-08-29T10:00:00Z"));
        assertThat(attempt.getPreferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);

        assertThat(attempt.getServiceOfferingId()).isNull();
        assertThat(attempt.getMatchedAt()).isNull();
    }

    @Test
    @DisplayName("동일한 실패 이벤트를 두 번 수신해도 매칭 이력이 한 건만 남는다")
    void 중복_수신은_처리되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);
        String failedAt = "2026-08-29T10:00:00Z";

        publish(carePlanId, servicePreferenceId, date, failedAt);
        await(() -> !springDataServiceMatchingAttemptRepository.findAll().isEmpty());

        // when
        publish(carePlanId, servicePreferenceId, date, failedAt);

        // then
        assertThat(stableAttemptCount()).isEqualTo(1);
    }

    // JSON을 실제 브로커로 발행
    private void publish(UUID carePlanId, UUID servicePreferenceId, LocalDate date, String failedAt) {
        String json = """
                {
                  "carePlanId": "%s",
                  "regionId": "%s",
                  "provideServiceId": "%s",
                  "servicePreferenceId": "%s",
                  "date": "%s",
                  "preferredTimeSlot": "MORNING",
                  "failureReason": "%s",
                  "failedAt": "%s"
                }
                """.formatted(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                date,
                FAILURE_REASON,
                failedAt
        );

        Message message = MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .andProperties(MessagePropertiesBuilder.newInstance()
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build())
                .build();

        rabbitTemplate.send(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                RabbitMqConfig.PROVIDER_MATCH_FAILED_ROUTING_KEY,
                message
        );
    }

    // RESCHEDULING 상태의 기존 일정을 저장
    private ServiceSchedule saveRescheduling(UUID carePlanId, UUID servicePreferenceId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );

        schedule.rescheduling();

        return springDataServiceScheduleRepository.save(schedule);
    }

    // 두 번째 메시지가 처리될 시간을 준 뒤의 이력 건수
    private int stableAttemptCount() {
        sleep(Duration.ofSeconds(2));

        return springDataServiceMatchingAttemptRepository.findAll().size();
    }

    // 리스너가 비동기로 동작하므로 조건이 만족될 때까지 짧게 폴링
    private void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plusSeconds(10);

        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }

            sleep(Duration.ofMillis(100));
        }

        throw new AssertionError("이벤트가 제한 시간 안에 처리되지 않았습니다.");
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
