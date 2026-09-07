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
        // UNDER_REVIEW 상태의 Care Plan 생성
        CarePlan carePlan = CarePlan.create(
                patientId,
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 10, 7),
                "방문간호 필요"
        );

        CarePlan savedCarePlan = carePlanCommandRepository.save(carePlan);

        // Care Plan에 포함될 서비스 항목 생성
        CarePlanService carePlanService = CarePlanService.create(
                savedCarePlan.getId(),
                provideServiceId
        );

        CarePlanService savedCarePlanService = carePlanServiceCommandRepository.save(carePlanService);

        // Provider 매칭에 사용할 희망 일정 생성
        CarePlanServicePreference preference = CarePlanServicePreference.create(
                savedCarePlanService.getId(),
                LocalDate.of(2026, 9, 10),
                PreferredTimeSlot.MORNING
        );

        CarePlanServicePreference savedPreference = servicePreferenceCommandRepository.save(preference);

        // Care Plan 확정 시 이벤트 payload에 포함될 환자의 지역 정보 Mocking
        given(userQueryPort.findById(patientId)).willReturn(new UserFindResult(patientId, UserRole.PATIENT, regionId));

        // 환자가 Care Plan을 UNDER_REVIEW -> CONFIRMED로 변경하는 요청
        CarePlanStatusUpdateCommand command = new CarePlanStatusUpdateCommand(
                userId,
                UserRole.PATIENT,
                savedCarePlan.getId(),
                CarePlanStatus.CONFIRMED
        );

        // 상태 변경 트랜잭션이 커밋되면 AFTER_COMMIT 이벤트 리스너를 통해
        // CarePlanConfirmedEvent가 RabbitMQ에 발행된다.
        carePlanCommandService.updateCarePlanStatus(command);

        // Care Plan 상태가 실제로 CONFIRMED로 변경되었는지 확인
        CarePlan updatedCarePlan = carePlanCommandRepository.findById(savedCarePlan.getId()).orElseThrow();

        assertThat(updatedCarePlan.getStatus()).isEqualTo(CarePlanStatus.CONFIRMED);

        // receiveAndConvert()를 사용하면 __TypeId__ 기반 역직렬화를 수행하므로
        // 발행 테스트에서는 Raw Message를 직접 받아 실제 JSON payload를 검증한다.
        Message message = rabbitTemplate.receive(
                TEST_QUEUE,
                5000
        );

        assertThat(message).isNotNull();

        JsonNode payload = objectMapper.readTree(message.getBody());

        // 이벤트 최상위 정보 검증
        assertThat(payload.get("carePlanId").asText()).isEqualTo(savedCarePlan.getId().toString());

        assertThat(payload.get("regionId").asText()).isEqualTo(regionId.toString());

        // 이벤트에 포함된 서비스 목록 검증
        JsonNode services = payload.get("services");

        assertThat(services).isNotNull();
        assertThat(services.isArray()).isTrue();
        assertThat(services.size()).isEqualTo(1);

        JsonNode service = services.get(0);

        assertThat(service.get("planServiceId").asText()).isEqualTo(savedCarePlanService.getId().toString());
        assertThat(service.get("provideServiceId").asText()).isEqualTo(provideServiceId.toString());

        // 해당 서비스에 등록된 희망 일정 목록 검증
        JsonNode preferences = service.get("preferences");

        assertThat(preferences).isNotNull();
        assertThat(preferences.isArray()).isTrue();
        assertThat(preferences.size()).isEqualTo(1);

        JsonNode preferencePayload = preferences.get(0);

        assertThat(preferencePayload.get("servicePreferenceId").asText()).isEqualTo(savedPreference.getId().toString());
        assertThat(preferencePayload.get("preferredDate").asText()).isEqualTo("2026-09-10");
        assertThat(preferencePayload.get("preferredTimeSlot").asText()).isEqualTo("MORNING");
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