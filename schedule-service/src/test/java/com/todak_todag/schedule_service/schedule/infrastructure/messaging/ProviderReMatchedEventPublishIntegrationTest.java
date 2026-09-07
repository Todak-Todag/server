package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.facade.ScheduleOutboxRelayFacade;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ProviderReMatched 발행 통합 테스트
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderReMatchedEventPublishIntegrationTest extends PostgresTestSupport {

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
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Autowired
    private ScheduleOutboxRelayFacade scheduleOutboxRelayFacade;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Autowired
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    // 테스트 간 아웃박스/큐 잔여물이 서로의 검증을 오염시키지 않도록 매번 비움
    @BeforeEach
    void clear() {
        springDataScheduleOutboxEventRepository.deleteAll();
        springDataServiceMatchingAttemptRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();

        while (rabbitTemplate.receive(RabbitMqConfig.PROVIDER_SCHEDULE_REMATCHED_QUEUE, 200) != null) {
            // 남아있는 메시지 비우기
        }
    }

    @Test
    @DisplayName("일정 변경(하루 미루기) 요청 시 문서 스펙대로 ProviderReMatched가 발행된다")
    void 일정을_미루면_이벤트가_발행된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID provideServiceId = UUID.randomUUID();

        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);
        recordMatchedAttempt(schedule, regionId, provideServiceId);

        LocalDate requestedDate = schedule.getDate().plusDays(1);

        // when
        ServiceScheduleRescheduleResult result = reschedule(schedule, requestedDate, carePlanId, patientId);
        scheduleOutboxRelayFacade.relay();

        // then
        assertThat(result.status()).isEqualTo(ScheduleStatus.RESCHEDULING);

        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo(
                "{\"carePlanId\":\"" + carePlanId + "\","
                        + "\"regionId\":\"" + regionId + "\","
                        + "\"provideServiceId\":\"" + provideServiceId + "\","
                        + "\"servicePreferenceId\":\"" + schedule.getServicePreferenceId() + "\","
                        + "\"date\":\"" + requestedDate + "\","
                        + "\"preferredTimeSlot\":null}"
        );
    }

    @Test
    @DisplayName("일정 변경(하루 앞당기기) 요청도 변경된 날짜로 발행된다")
    void 일정을_앞당겨도_변경된_날짜로_발행된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);
        recordMatchedAttempt(schedule, UUID.randomUUID(), UUID.randomUUID());

        LocalDate requestedDate = schedule.getDate().minusDays(1);

        // when
        reschedule(schedule, requestedDate, carePlanId, patientId);
        scheduleOutboxRelayFacade.relay();

        // then
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).contains("\"date\":\"" + requestedDate + "\"");
        assertThat(received.getFirst()).doesNotContain("\"date\":\"" + schedule.getDate() + "\"");
    }

    @Test
    @DisplayName("매칭 시도 기록이 없으면 발행되지 않고 일정도 SCHEDULED로 남는다")
    void 매칭_시도_기록이_없으면_발행되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);

        // when & then
        assertThatThrownBy(() -> reschedule(schedule, schedule.getDate().plusDays(1), carePlanId, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_MATCHING_ATTEMPT_NOT_FOUND);

        scheduleOutboxRelayFacade.relay();

        assertThat(receiveAll()).isEmpty();
        assertThat(springDataServiceScheduleRepository.findById(schedule.getId()))
                .get()
                .extracting(ServiceSchedule::getStatus)
                .isEqualTo(ScheduleStatus.SCHEDULED);
    }

    private ServiceScheduleRescheduleResult reschedule(
            ServiceSchedule schedule,
            LocalDate requestedDate,
            UUID carePlanId,
            UUID patientId
    ) {
        return serviceScheduleCommandService.reschedule(
                new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId),
                new CarePlanPort.CarePlanRange(carePlanId, schedule.getDate().plusDays(30), patientId)
        );
    }

    // SCHEDULED 상태의 일정 저장
    private ServiceSchedule scheduledSchedule(UUID carePlanId, int plusDays) {
        LocalDate date = LocalDate.now().plusDays(plusDays);

        return springDataServiceScheduleRepository.save(
                ServiceSchedule.confirm(
                        carePlanId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        date,
                        date.atTime(9, 0),
                        date.atTime(10, 0)
                )
        );
    }

    // 이 일정을 성사시킨 매칭 시도 기록
    private void recordMatchedAttempt(ServiceSchedule schedule, UUID regionId, UUID provideServiceId) {
        springDataServiceMatchingAttemptRepository.save(
                ServiceMatchingAttempt.record(
                        schedule.getCarePlanId(),
                        regionId,
                        provideServiceId,
                        schedule.getServicePreferenceId(),
                        schedule.getServiceOfferingId(),
                        schedule.getDate(),
                        null,
                        MatchingAttemptStatus.MATCHED,
                        null,
                        Instant.now(),
                        null
                )
        );
    }

    // 문서에 명시된 큐에 도착한 메시지 본문을 모두 꺼냄
    private List<String> receiveAll() {
        List<String> payloads = new ArrayList<>();

        Message message;
        while ((message = rabbitTemplate.receive(RabbitMqConfig.PROVIDER_SCHEDULE_REMATCHED_QUEUE, 500)) != null) {
            payloads.add(new String(message.getBody()));
        }

        return payloads;
    }
}
