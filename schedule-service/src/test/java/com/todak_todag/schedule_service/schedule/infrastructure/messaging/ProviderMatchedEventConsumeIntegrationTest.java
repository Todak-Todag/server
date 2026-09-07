package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
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

// ProviderMatched 수신 통합 테스트
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderMatchedEventConsumeIntegrationTest extends PostgresTestSupport {

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
    @DisplayName("신규 매칭 이벤트를 수신하면 서비스 일정이 SCHEDULED로 생성된다")
    void 신규_매칭_이벤트로_일정이_생성된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        // when
        publish(carePlanId, servicePreferenceId, serviceOfferingId, date, "2026-08-29T10:00:00Z");

        // then
        await(() -> !springDataServiceScheduleRepository.findAll().isEmpty());

        List<ServiceSchedule> schedules = springDataServiceScheduleRepository.findAll();
        assertThat(schedules).hasSize(1);

        ServiceSchedule created = schedules.getFirst();
        assertThat(created.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(created.getCarePlanId()).isEqualTo(carePlanId);
        assertThat(created.getServicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(created.getServiceOfferingId()).isEqualTo(serviceOfferingId);
        assertThat(created.getDate()).isEqualTo(date);
        assertThat(created.getStartedAt()).isEqualTo(date.atTime(10, 0));
        assertThat(created.getFinishedAt()).isEqualTo(date.atTime(11, 0));
    }

    @Test
    @DisplayName("재매칭 이벤트를 수신하면 기존 RESCHEDULING 일정이 CHANGED가 되고 새 일정이 생성된다")
    void 재매칭_이벤트로_기존_일정이_변경완료된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate originalDate = LocalDate.now().plusDays(5);

        ServiceSchedule existing = saveRescheduling(carePlanId, servicePreferenceId, originalDate);

        // when
        LocalDate rematchedDate = originalDate.plusDays(1);
        UUID newServiceOfferingId = UUID.randomUUID();

        publish(carePlanId, servicePreferenceId, newServiceOfferingId, rematchedDate, "2026-08-30T10:00:00Z");

        // then
        await(() -> springDataServiceScheduleRepository.findAll().size() == 2);

        ServiceSchedule closed = springDataServiceScheduleRepository.findById(existing.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(ScheduleStatus.CHANGED);

        assertThat(closed.getDate()).isEqualTo(originalDate);

        ServiceSchedule created = springDataServiceScheduleRepository.findAll().stream()
                .filter(schedule -> !schedule.getId().equals(existing.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(created.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(created.getDate()).isEqualTo(rematchedDate);
        assertThat(created.getServiceOfferingId()).isEqualTo(newServiceOfferingId);
    }

    @Test
    @DisplayName("수신한 매칭 결과가 p_service_matching_attempts에 기록된다")
    void 매칭_시도_결과가_기록된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        // when
        publish(carePlanId, servicePreferenceId, serviceOfferingId, date, "2026-08-29T10:00:00Z");

        // then
        await(() -> !springDataServiceMatchingAttemptRepository.findAll().isEmpty());

        List<ServiceMatchingAttempt> attempts = springDataServiceMatchingAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);

        ServiceMatchingAttempt attempt = attempts.getFirst();
        assertThat(attempt.getCarePlanId()).isEqualTo(carePlanId);
        assertThat(attempt.getServicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(attempt.getServiceOfferingId()).isEqualTo(serviceOfferingId);
        assertThat(attempt.getDate()).isEqualTo(date);
        assertThat(attempt.getStatus()).isEqualTo(MatchingAttemptStatus.MATCHED);
        assertThat(attempt.getMatchedAt()).isEqualTo(Instant.parse("2026-08-29T10:00:00Z"));

        assertThat(attempt.getPreferredTimeSlot()).isNull();
        assertThat(attempt.getFailureReason()).isNull();
        assertThat(attempt.getFailedAt()).isNull();
    }

    @Test
    @DisplayName("동일한 이벤트를 두 번 수신해도 일정과 매칭 이력이 한 건씩만 남는다")
    void 중복_수신은_처리되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);
        String matchedAt = "2026-08-29T10:00:00Z";

        publish(carePlanId, servicePreferenceId, serviceOfferingId, date, matchedAt);
        await(() -> !springDataServiceScheduleRepository.findAll().isEmpty());

        // when
        publish(carePlanId, servicePreferenceId, serviceOfferingId, date, matchedAt);

        // then
        assertThat(stableCount()).isEqualTo(1);
        assertThat(springDataServiceMatchingAttemptRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("발행 측 클래스명이 담긴 __TypeId__ 헤더가 있어도 리스너 파라미터 타입으로 역직렬화한다")
    void 발행측_TypeId_헤더를_무시하고_수신한다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(5);

        // when
        publishWithTypeId(
                carePlanId,
                servicePreferenceId,
                serviceOfferingId,
                date,
                "2026-08-29T10:00:00Z",
                "com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent"
        );

        // then
        await(() -> !springDataServiceScheduleRepository.findAll().isEmpty());

        List<ServiceSchedule> schedules = springDataServiceScheduleRepository.findAll();
        assertThat(schedules).hasSize(1);
        assertThat(schedules.getFirst().getServicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(schedules.getFirst().getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
    }

    // 동일한 형태의 JSON을 실제 브로커로 발행
    private void publish(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            String matchedAt
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
                  "matchedAt": "%s"
                }
                """.formatted(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                serviceOfferingId,
                date,
                date,
                matchedAt
        );

        Message message = MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .andProperties(MessagePropertiesBuilder.newInstance()
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build())
                .build();

        rabbitTemplate.send(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                RabbitMqConfig.PROVIDER_MATCHED_ROUTING_KEY,
                message
        );
    }

    // publish와 같은 페이로드에 발행 측 클래스명을 담은 __TypeId__ 헤더만 추가
    private void publishWithTypeId(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            String matchedAt,
            String typeId
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
                  "matchedAt": "%s"
                }
                """.formatted(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                serviceOfferingId,
                date,
                date,
                matchedAt
        );

        Message message = MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .andProperties(MessagePropertiesBuilder.newInstance()
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setHeader("__TypeId__", typeId)
                        .build())
                .build();

        rabbitTemplate.send(
                RabbitMqConfig.PROVIDER_EXCHANGE,
                RabbitMqConfig.PROVIDER_MATCHED_ROUTING_KEY,
                message
        );
    }

    // RESCHEDULING 상태의 기존 일정을 생성
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

    // 두 번째 메시지가 처리될 시간을 준 뒤의 일정 건수
    // 중복이 걸러졌다면 계속 1건, 걸러지지 않았다면 2건
    private int stableCount() {
        sleep(Duration.ofSeconds(2));

        return springDataServiceScheduleRepository.findAll().size();
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
