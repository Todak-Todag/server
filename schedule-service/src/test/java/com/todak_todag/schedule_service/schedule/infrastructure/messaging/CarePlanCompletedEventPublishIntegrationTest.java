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
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 3);
        ServiceSchedule last = completedSchedule(carePlanId, 5);
        registerResult(earlier);

        // when
        ServiceResultRegisterResult lastResult = registerResult(last);
        scheduleOutboxRelayFacade.relay();

        // then
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo(
                "{\"serviceResultId\":\"" + lastResult.serviceResultId() + "\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("앞선 일정의 결과만 등록된 시점에는 마지막 일정이 남아있으므로 발행되지 않는다")
    void 아직_일정이_남아있으면_발행되지_않는다() {
        // given
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
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 3);
        ServiceSchedule last = scheduledSchedule(carePlanId, 5);
        registerResult(earlier);

        // when
        serviceScheduleCommandService.cancel(
                new ServiceScheduleCancelCommand(last.getId(), "재매칭 실패", patientId),
                new CarePlanPort.CarePlanRange(carePlanId, LocalDate.now().plusDays(10), patientId)
        );
        scheduleOutboxRelayFacade.relay();

        // then
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    @Test
    @DisplayName("뒤 일정이 먼저 취소되고 앞 일정이 나중에 끝나도 케어플랜 완료가 발행된다")
    void 결말_순서가_날짜_순서와_어긋나도_발행된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule earlierA = scheduledSchedule(carePlanId, 3);
        ServiceSchedule laterB = scheduledSchedule(carePlanId, 5);

        serviceScheduleCommandService.cancel(
                new ServiceScheduleCancelCommand(laterB.getId(), "개인 사정", patientId),
                new CarePlanPort.CarePlanRange(carePlanId, LocalDate.now().plusDays(10), patientId)
        );
        scheduleOutboxRelayFacade.relay();
        assertThat(receiveAll()).isEmpty();

        // when
        earlierA.complete();
        springDataServiceScheduleRepository.save(earlierA);
        registerResult(earlierA);
        scheduleOutboxRelayFacade.relay();

        // then
        List<String> received = receiveAll();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    @Test
    @DisplayName("모든 일정이 끝난 뒤 앞선 일정의 결과가 뒤늦게 등록되어도 중복 발행되지 않는다")
    void 이미_발행된_케어플랜은_중복_발행되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlierA = completedSchedule(carePlanId, 3);
        ServiceSchedule laterB = completedSchedule(carePlanId, 5);

        registerResult(laterB);
        scheduleOutboxRelayFacade.relay();
        assertThat(receiveAll()).hasSize(1);

        // when
        registerResult(earlierA);
        scheduleOutboxRelayFacade.relay();

        // then
        assertThat(receiveAll()).isEmpty();
    }

    // 수행 결과 등록
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

    // 수행 완료 처리 COMPLETED로 만들어 저장
    private ServiceSchedule completedSchedule(UUID carePlanId, int plusDays) {
        ServiceSchedule schedule = scheduledSchedule(carePlanId, plusDays);
        schedule.complete();

        return springDataServiceScheduleRepository.save(schedule);
    }

    // 문서에 명시된 큐에 도착한 메시지 본문을 모두 꺼냄
    private List<String> receiveAll() {
        List<String> payloads = new ArrayList<>();

        org.springframework.amqp.core.Message message;
        while ((message = rabbitTemplate.receive(RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_QUEUE, 500)) != null) {
            payloads.add(new String(message.getBody()));
        }

        return payloads;
    }
}
