package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.global.config.RabbitMqConfig;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.facade.ScheduleOutboxRelayFacade;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceResultCommandService;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceScheduleCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// CarePlanCompleted 발행 통합 테스트
//
// 검증 범위: 커맨드 트랜잭션(발행 조건 판단 → 아웃박스 적재) → 릴레이 → 실제 RabbitMQ 발행 → 큐 수신
// 실제 브로커를 Testcontainers로 띄우기 때문에 문서에 명시된 Exchange/Routing Key/Queue로
// 정말 라우팅되는지까지 확인할 수 있다(Mock으로는 검증되지 않는 부분).
//
// 릴레이 스케줄러는 test 프로필에서 꺼져 있으므로(schedule.outbox.relay.enabled=false)
// ScheduleOutboxRelayFacade.relay()를 직접 호출해 발행 시점을 결정적으로 만든다.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CarePlanCompletedEventPublishIntegrationTest {

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
    private ServiceResultCommandService serviceResultCommandService;

    @Autowired
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Autowired
    private ScheduleOutboxRelayFacade scheduleOutboxRelayFacade;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Autowired
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    // 테스트 간 아웃박스/큐 잔여물이 서로의 검증을 오염시키지 않도록 매번 비운다
    @BeforeEach
    void clear() {
        springDataScheduleOutboxEventRepository.deleteAll();
        springDataCarePlanServiceResultRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();

        while (rabbitTemplate.receive(RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_QUEUE, 200) != null) {
            // 남아있는 메시지 비우기
        }
    }

    @Test
    @DisplayName("케어플랜의 마지막 일정이 정상 수행 완료되면 문서 스펙대로 CarePlanCompleted가 발행된다")
    void 마지막_일정이_수행_완료되면_이벤트가_발행된다() {
        // given — 같은 케어플랜에 일정 2건, 앞선 일정은 이미 결과까지 등록되어 있다
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 3);
        ServiceSchedule last = completedSchedule(carePlanId, 5);
        registerResult(earlier);

        // when — 마지막 일정의 수행 결과를 등록하고 릴레이를 돌린다
        ServiceResultRegisterResult lastResult = registerResult(last);
        scheduleOutboxRelayFacade.relay();

        // then — 문서에 명시된 큐로 serviceResultId + status 페이로드가 도착한다
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo(
                "{\"serviceResultId\":\"" + lastResult.serviceResultId() + "\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("앞선 일정의 결과만 등록된 시점에는 마지막 일정이 남아있으므로 발행되지 않는다")
    void 아직_일정이_남아있으면_발행되지_않는다() {
        // given — 마지막 일정은 아직 SCHEDULED 상태로 남아있다
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 3);
        scheduledSchedule(carePlanId, 5);

        // when
        registerResult(earlier);
        scheduleOutboxRelayFacade.relay();

        // then
        assertThat(receiveAll()).isEmpty();
    }

    @Test
    @DisplayName("마지막 일정이 취소되어 남은 일정이 없으면 serviceResultId는 null, status는 CANCELED로 발행된다")
    void 마지막_일정이_취소되면_CANCELED로_발행된다() {
        // given — 앞선 일정은 수행 결과까지 등록, 마지막 일정은 아직 SCHEDULED(취소 가능)
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 3);
        ServiceSchedule last = scheduledSchedule(carePlanId, 5);
        registerResult(earlier);

        // when — 마지막 일정을 취소한다 (재매칭 실패로 더 이상 남은 일정이 없는 상황)
        serviceScheduleCommandService.cancel(
                new ServiceScheduleCancelCommand(last.getId(), "재매칭 실패", patientId),
                new CarePlanPort.CarePlanRange(carePlanId, LocalDate.now().plusDays(10), patientId)
        );
        scheduleOutboxRelayFacade.relay();

        // then — 취소된 일정은 수행된 적이 없으므로 결과 ID가 없다
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    // 수행 결과 등록 — 배정된 제공자 검증을 통과시키기 위해 요청자와 배정 제공자를 같은 값으로 넘긴다
    private ServiceResultRegisterResult registerResult(ServiceSchedule schedule) {
        UUID providerId = UUID.randomUUID();

        return serviceResultCommandService.register(
                new ServiceResultRegisterCommand(
                        schedule.getId(),
                        schedule.getStartedAt(),
                        schedule.getFinishedAt(),
                        null,
                        providerId
                ),
                providerId
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

    // 07번은 05번(수행 완료 처리)이 선행되어야 하므로 COMPLETED로 만들어 저장
    private ServiceSchedule completedSchedule(UUID carePlanId, int plusDays) {
        ServiceSchedule schedule = scheduledSchedule(carePlanId, plusDays);
        schedule.complete();

        return springDataServiceScheduleRepository.save(schedule);
    }

    // 문서에 명시된 큐에 도착한 메시지 본문을 모두 꺼낸다
    private List<String> receiveAll() {
        List<String> payloads = new ArrayList<>();

        org.springframework.amqp.core.Message message;
        while ((message = rabbitTemplate.receive(RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_QUEUE, 500)) != null) {
            payloads.add(new String(message.getBody()));
        }

        return payloads;
    }
}
