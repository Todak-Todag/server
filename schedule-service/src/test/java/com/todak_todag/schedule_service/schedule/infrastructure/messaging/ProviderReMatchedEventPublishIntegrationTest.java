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
//
// 검증 범위: 03번 API의 커맨드 트랜잭션(RESCHEDULING 전이 → 아웃박스 적재) → 릴레이 → 실제 RabbitMQ 발행 → 큐 수신
// 11번(CarePlanCompleted) 통합 테스트에서 정한 방식을 그대로 재사용한다:
//   - Testcontainers로 실제 브로커를 띄워 문서에 명시된 Exchange/Routing Key/Queue로 정말 라우팅되는지까지 확인
//   - 릴레이 스케줄러는 test 프로필에서 꺼져 있으므로(schedule.outbox.relay.enabled=false)
//     ScheduleOutboxRelayFacade.relay()를 직접 호출해 발행 시점을 결정적으로 만든다
//
// 이번 범위는 03번(일정 변경) 경로뿐이다. 12번 문서의 또 다른 발행 시나리오인 "재매칭 시도 API"는
// 01~14번 어디에도 문서화되어 있지 않아 구현 대상에서 제외했다(schedule-service.md 5.1절 ⚠️).
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProviderReMatchedEventPublishIntegrationTest {

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

    // 테스트 간 아웃박스/큐 잔여물이 서로의 검증을 오염시키지 않도록 매번 비운다
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
        // given — SCHEDULED 일정 1건과 그 일정을 성사시킨 MATCHED 매칭 시도 1건
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID provideServiceId = UUID.randomUUID();

        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);
        recordMatchedAttempt(schedule, regionId, provideServiceId);

        LocalDate requestedDate = schedule.getDate().plusDays(1);

        // when — 03번 API의 커맨드 트랜잭션을 태우고 릴레이를 돌린다
        ServiceScheduleRescheduleResult result = reschedule(schedule, requestedDate, carePlanId, patientId);
        scheduleOutboxRelayFacade.relay();

        // then — 일정은 RESCHEDULING 중간 상태가 되고
        assertThat(result.status()).isEqualTo(ScheduleStatus.RESCHEDULING);

        // 문서에 명시된 큐로 페이로드 표와 동일한 JSON이 도착한다 (preferredTimeSlot은 03번 경로이므로 null)
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

        // then — 기존 날짜가 아니라 "재매칭을 원하는 날짜"가 실린다
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).contains("\"date\":\"" + requestedDate + "\"");
        assertThat(received.getFirst()).doesNotContain("\"date\":\"" + schedule.getDate() + "\"");
    }

    @Test
    @DisplayName("매칭 시도 기록이 없으면 발행되지 않고 일정도 SCHEDULED로 남는다")
    void 매칭_시도_기록이_없으면_발행되지_않는다() {
        // given — regionId/provideServiceId를 얻을 곳이 없는 상황 (매칭 시도 기록을 만들지 않는다)
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);

        // when & then — 이벤트만 건너뛰는 게 아니라 일정 변경 자체가 실패한다
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

    // 03번 API의 커맨드 트랜잭션 호출
    // carePlanRange는 Facade가 care-plan-service Internal API(5.5절)로 채워 넘기는 값이라
    // 이 테스트에서는 검증을 통과하는 값으로 직접 구성한다 (요청자 = 소유자, 일정 범위는 충분히 넉넉하게)
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
    // 실제 운영에서는 13번(ProviderMatched 수신)이 남기지만 아직 미구현이라 테스트에서 직접 심는다
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

    // 문서에 명시된 큐에 도착한 메시지 본문을 모두 꺼낸다
    private List<String> receiveAll() {
        List<String> payloads = new ArrayList<>();

        Message message;
        while ((message = rabbitTemplate.receive(RabbitMqConfig.PROVIDER_SCHEDULE_REMATCHED_QUEUE, 500)) != null) {
            payloads.add(new String(message.getBody()));
        }

        return payloads;
    }
}
