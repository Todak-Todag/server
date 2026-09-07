package com.spring.careplanservice.careplan.infrastructure.messaging;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.UserFindResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanCommandService;
import com.spring.careplanservice.careplan.domain.entity.*;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class CarePlanConfirmedEventPublishIntegrationTest extends IntegrationTestSupport {
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
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @MockitoBean
    private UserQueryPort userQueryPort;

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

        declareTestQueue();
    }

    @Test
    @DisplayName("Care Plan이 UNDER_REVIEW에서 CONFIRMED로 변경되면 CarePlanConfirmed 이벤트 발행")
    void carePlanConfirmedEventPublish_success() throws Exception {
        CarePlan carePlan = CarePlan.create(
                patientId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 10, 7),
                "방문간호 필요"
        );

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        CarePlanService carePlanService =
                CarePlanService.create(
                        savedCarePlan.getId(),
                        provideServiceId
                );

        CarePlanService savedCarePlanService = carePlanServiceCommandRepository.save(carePlanService);

        CarePlanServicePreference preference = CarePlanServicePreference.create(
                savedCarePlanService.getId(),
                LocalDate.of(2026, 9, 10),
                PreferredTimeSlot.MORNING
        );

        CarePlanServicePreference savedPreference = servicePreferenceCommandRepository.save(preference);

        given(userQueryPort.findById(patientId)).willReturn(new UserFindResult(
                patientId, UserRole.PATIENT, regionId));

        CarePlanStatusUpdateCommand command = new CarePlanStatusUpdateCommand(
                userId,
                UserRole.PATIENT,
                savedCarePlan.getId(),
                CarePlanStatus.CONFIRMED
        );

        carePlanCommandService.updateCarePlanStatus(command);

        CarePlan updatedCarePlan = carePlanCommandRepository.findById(savedCarePlan.getId()).orElseThrow();

        assertThat(updatedCarePlan.getStatus()).isEqualTo(CarePlanStatus.CONFIRMED);

        Message message = rabbitTemplate.receive(
                TEST_QUEUE,
                5000
        );

        assertThat(message).isNotNull();

        JsonNode payload = objectMapper.readTree(
                message.getBody()
        );

        assertThat(payload.get("carePlanId").asText())
                .isEqualTo(savedCarePlan.getId().toString());

        assertThat(payload.get("regionId").asText())
                .isEqualTo(regionId.toString());
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

    private void declareTestQueue() {
        Queue queue = new Queue(
                TEST_QUEUE,
                false,
                true,
                true
        );

        amqpAdmin.declareQueue(queue);

        Binding binding =
                BindingBuilder.bind(queue)
                        .to(
                                new DirectExchange(
                                        RabbitMqConfig.CARE_PLAN_CONFIRMED_EXCHANGE
                                )
                        )
                        .with(
                                RabbitMqConfig.CARE_PLAN_CONFIRMED_ROUTING_KEY
                        );

        amqpAdmin.declareBinding(binding);
    }
}