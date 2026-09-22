package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEventAppender;
import com.spring.careplanservice.careplan.application.port.CarePlanConfirmedEventPort;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.careplan.domain.entity.*;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.SpringDataCarePlanOutboxEventRepository;
import com.spring.careplanservice.careplan.support.IntegrationTestSupport;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import com.spring.careplanservice.global.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class CarePlanConfirmedEventPublishIntegrationTest extends IntegrationTestSupport {
    /*
    1) CarePlanConfirmed Outbox 적재 테스트
       = Care Plan이 CONFIRMED 되면 같은 트랜잭션 안에서
         CARE_PLAN_CONFIRMED 타입의 Outbox row가 PENDING으로 저장되는지 검증
    2) 트랜잭션 롤백 테스트
       = CONFIRMED 처리 도중 예외가 발생하면
         Care Plan 상태 변경과 Outbox 적재가 함께 롤백되는지 검증
    3) CarePlanConfirmedEventRabbitAdapter 발행 테스트
       = Outbox Relay가 실제로 호출하는 Port/Adapter가
         기존 care-plan.exchange / care-plan.confirmed.key로 정상 발행하는지 검증
    */

    private static final String TEST_QUEUE = "test.care-plan-confirmed.queue";

    @Autowired
    private CarePlanCommandService carePlanCommandService;

    @Autowired
    private CarePlanCommandRepository carePlanCommandRepository;

    @Autowired
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @Autowired
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

    @Autowired
    private SpringDataCarePlanOutboxEventRepository springDataCarePlanOutboxEventRepository;

    @Autowired
    private CarePlanConfirmedEventPort carePlanConfirmedEventPort;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoSpyBean
    private CarePlanConfirmedEventAppender carePlanConfirmedEventAppender;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID patientId;
    private UUID userId;
    private UUID regionId;
    private UUID provideServiceId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        userId = patientId;
        regionId = UUID.randomUUID();
        provideServiceId = UUID.randomUUID();

        setAuthentication();
    }

    @Test
    @DisplayName("Care Plan이 UNDER_REVIEW에서 CONFIRMED로 변경되면 같은 트랜잭션에서 CARE_PLAN_CONFIRMED Outbox row가 PENDING으로 저장된다")
    void carePlanConfirmedOutboxEvent_created_success() throws Exception {
        CarePlan savedCarePlan = createUnderReviewCarePlanWithService();


        CarePlanStatusUpdateCommand command = new CarePlanStatusUpdateCommand(
                userId,
                UserRole.PATIENT,
                savedCarePlan.getId(),
                CarePlanStatus.CONFIRMED
        );

        carePlanCommandService.updateCarePlanStatus(
                command,
                regionId
        );

        CarePlan updatedCarePlan = carePlanCommandRepository.findById(savedCarePlan.getId()).orElseThrow();
        assertThat(updatedCarePlan.getStatus()).isEqualTo(CarePlanStatus.CONFIRMED);

        CarePlanOutboxEvent outboxEvent = findOutboxEventByAggregateId(savedCarePlan.getId());

        assertThat(outboxEvent.getEventType()).isEqualTo(CarePlanOutboxEventType.CARE_PLAN_CONFIRMED);
        assertThat(outboxEvent.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);

        JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(payload.get("carePlanId").asText()).isEqualTo(savedCarePlan.getId().toString());
        assertThat(payload.get("regionId").asText()).isEqualTo(regionId.toString());
    }

    @Test
    @DisplayName("CONFIRMED 처리 중 예외가 발생하면 Care Plan 상태 변경과 Outbox 적재가 함께 롤백된다")
    void updateCarePlanStatus_rollsBackTogetherWithOutbox_whenTransactionFails() {
        CarePlan savedCarePlan = createUnderReviewCarePlanWithService();

        CarePlanStatusUpdateCommand command = new CarePlanStatusUpdateCommand(
                userId,
                UserRole.PATIENT,
                savedCarePlan.getId(),
                CarePlanStatus.CONFIRMED
        );

        // 실제 Outbox 저장 로직까지 수행한 뒤 예외를 발생시켜
        // 동일 트랜잭션의 상태 변경과 Outbox 적재가 함께 롤백되는지 검증한다.
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new RuntimeException("Outbox 적재 후 테스트 예외");
        }).when(carePlanConfirmedEventAppender).append(any(CarePlanConfirmedEvent.class));

        assertThatThrownBy(() -> carePlanCommandService.updateCarePlanStatus(
                command,
                regionId
        )).isInstanceOf(RuntimeException.class);
        CarePlan carePlanAfterFailure = carePlanCommandRepository
                .findById(savedCarePlan.getId())
                .orElseThrow();

        assertThat(carePlanAfterFailure.getStatus()).isEqualTo(CarePlanStatus.UNDER_REVIEW);

        boolean outboxEventCreated = springDataCarePlanOutboxEventRepository
                .findAll()
                .stream()
                .anyMatch(event ->
                        event.getAggregateId().equals(savedCarePlan.getId())
                );

        assertThat(outboxEventCreated).isFalse();
    }

    @Test
    @DisplayName("CarePlanConfirmedEventRabbitAdapter는 CarePlanConfirmed 이벤트를 기존 care-plan.exchange / care-plan.confirmed.key로 발행한다")
    void carePlanConfirmedEventPort_publish_success() throws Exception {
        UUID eventId = UUID.randomUUID();

        Queue queue = QueueBuilder
                .nonDurable(TEST_QUEUE)
                .exclusive()
                .autoDelete()
                .build();

        amqpAdmin.declareQueue(queue);

        Binding binding = BindingBuilder
                .bind(queue)
                .to(new DirectExchange(RabbitMqConfig.CARE_PLAN_CONFIRMED_EXCHANGE))
                .with(RabbitMqConfig.CARE_PLAN_CONFIRMED_ROUTING_KEY);

        amqpAdmin.declareBinding(binding);

        UUID carePlanId = UUID.randomUUID();

        CarePlanConfirmedEvent event = new CarePlanConfirmedEvent(
                eventId,
                carePlanId,
                regionId,
                List.of(
                        new CarePlanConfirmedEvent.Service(
                                UUID.randomUUID(),
                                provideServiceId,
                                List.of(
                                        new CarePlanConfirmedEvent.Preference(
                                                UUID.randomUUID(),
                                                LocalDate.of(2026, 9, 10),
                                                PreferredTimeSlot.MORNING
                                        )
                                )
                        )
                )
        );

        carePlanConfirmedEventPort.publish(event);

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5000);

        assertThat(message).isNotNull();

        JsonNode payload = objectMapper.readTree(message.getBody());

        assertThat(payload.get("carePlanId").asText()).isEqualTo(carePlanId.toString());
        assertThat(payload.get("regionId").asText()).isEqualTo(regionId.toString());

        JsonNode services = payload.get("services");
        assertThat(services.isArray()).isTrue();
        assertThat(services.size()).isEqualTo(1);
    }

    private CarePlan createUnderReviewCarePlanWithService() {
        CarePlan carePlan = CarePlan.create(
                patientId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 10, 7),
                "방문간호 필요"
        );

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        CarePlanService carePlanService = CarePlanService.create(
                savedCarePlan.getId(),
                provideServiceId
        );

        CarePlanService savedCarePlanService = carePlanServiceCommandRepository.save(carePlanService);

        servicePreferenceCommandRepository.save(
                CarePlanServicePreference.create(
                        savedCarePlanService.getId(),
                        LocalDate.of(2026, 9, 10),
                        PreferredTimeSlot.MORNING
                )
        );

        return savedCarePlan;
    }

    private CarePlanOutboxEvent findOutboxEventByAggregateId(UUID aggregateId) {
        return springDataCarePlanOutboxEventRepository.findAll().stream()
                .filter(event -> event.getAggregateId().equals(aggregateId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "aggregateId=" + aggregateId + "에 대한 Outbox 이벤트가 존재하지 않습니다."
                ));
    }

    private void setAuthentication() {
        UserContext userContext = new UserContext(
                userId,
                UserRole.PATIENT
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userContext,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
