package com.spring.careplanservice.careplan.infrastructure.messaging;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.event.ScheduleStatus;
import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.support.IntegrationTestSupport;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import com.spring.careplanservice.global.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class CarePlanCompletedEventConsumeIntegrationTest extends IntegrationTestSupport {
    /*
     2) CarePlanCompletedEventConsumeIntegrationTest
       = 이벤트 수신 측 테스트
       = RabbitMQ로 CarePlanCompleted 이벤트를 넣었을 때
         Care Plan이 COMPLETED로 바뀌는지 검증
    */
    @Autowired
    private CarePlanCommandRepository carePlanCommandRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ScheduleResultQueryPort scheduleResultQueryPort;

    private UUID userId;
    private UUID patientId;
    private UUID carePlanId;
    private UUID serviceResultId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        serviceResultId = UUID.randomUUID();

        setAuthentication();
    }

    @Test
    @DisplayName("COMPLETED 일정 이벤트를 수신하면 IN_PROGRESS Care Plan이 COMPLETED 변경")
    void carePlanCompletedEventConsume_success() throws Exception {
        // 완료 이벤트를 받을 수 있도록 IN_PROGRESS 상태의 Care Plan 준비
        CarePlan carePlan = CarePlan.create(
                patientId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 10, 7),
                "방문간호 필요"
        );

        carePlan.updateStatus(CarePlanStatus.CONFIRMED);
        carePlan.updateStatus(CarePlanStatus.IN_PROGRESS);

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        carePlanId = savedCarePlan.getId();

        // serviceResultId가 Schedule-Service에 실제 존재한다고 가정
        given(scheduleResultQueryPort.findById(serviceResultId))
                .willReturn(
                        new ScheduleResultFindResult(
                                serviceResultId
                        )
                );

        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                serviceResultId,
                ScheduleStatus.COMPLETED
        );

        // Schedule-Service가 발행하는 것과 동일한 Exchange / Routing Key로 이벤트 발행
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCHEDULE_EXCHANGE,
                RabbitMqConfig.SCHEDULE_COMPLETED_ROUTING_KEY,
                event
        );

        // RabbitListener가 비동기로 처리하므로 상태 변경을 잠시 기다린다.
        CarePlan completedCarePlan = waitUntilCompleted(carePlanId);

        assertThat(completedCarePlan.getStatus()).isEqualTo(CarePlanStatus.COMPLETED);
        verify(scheduleResultQueryPort).findById(serviceResultId);
    }

    private CarePlan waitUntilCompleted(UUID carePlanId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            CarePlan carePlan = carePlanCommandRepository
                    .findById(carePlanId)
                    .orElseThrow();

            if (carePlan.getStatus() == CarePlanStatus.COMPLETED) {
                return carePlan;
            }

            Thread.sleep(100);
        }

        return carePlanCommandRepository
                .findById(carePlanId)
                .orElseThrow();
    }

    private void setAuthentication() {
        // JPA Auditing의 createdBy / updatedBy 값을 채우기 위한 테스트 인증 정보
        UserContext userContext = new UserContext(
                userId,
                UserRole.SERVICE_PROVIDER
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userContext,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("CANCELED 일정 이벤트에서 serviceResultId가 null이면 수행 결과 조회 없이 Care Plan이 COMPLETED 변경")
    void carePlanCanceledEventConsume_success() throws Exception {
        // 취소 이벤트를 받을 수 있도록 IN_PROGRESS 상태의 Care Plan 준비
        CarePlan carePlan = CarePlan.create(
                patientId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 10, 7),
                "방문간호 필요"
        );

        carePlan.updateStatus(CarePlanStatus.CONFIRMED);
        carePlan.updateStatus(CarePlanStatus.IN_PROGRESS);

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        UUID savedCarePlanId = savedCarePlan.getId();

        // CANCELED 일정은 serviceResultId가 생성되지 않을 수 있으므로 null 전달
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                savedCarePlanId,
                null,
                ScheduleStatus.CANCELED
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCHEDULE_EXCHANGE,
                RabbitMqConfig.SCHEDULE_COMPLETED_ROUTING_KEY,
                event
        );

        CarePlan completedCarePlan = waitUntilCompleted(savedCarePlanId);

        assertThat(completedCarePlan.getStatus()).isEqualTo(CarePlanStatus.COMPLETED);
    }
}