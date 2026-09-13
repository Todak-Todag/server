package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEventPayloadSerializer;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEventPayloadSerializer;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import com.spring.careplanservice.careplan.application.port.CarePlanConfirmedEventPort;
import com.spring.careplanservice.careplan.application.result.CarePlanOutboxEventResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanOutboxCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanOutboxQueryService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanOutboxRelayFacadeTest {
    UUID confirmedOutboxEventId = UUID.randomUUID();
    UUID completedOutboxEventId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();

    @Mock
    private CarePlanOutboxQueryService carePlanOutboxQueryService;

    @Mock
    private CarePlanOutboxCommandService carePlanOutboxCommandService;

    @Mock
    private CarePlanCompletionEventPayloadSerializer carePlanCompletionEventPayloadSerializer;

    @Mock
    private CarePlanCompletedEventPort carePlanCompletedEventPort;

    @Mock
    private CarePlanConfirmedEventPayloadSerializer carePlanConfirmedEventPayloadSerializer;

    @Mock
    private CarePlanConfirmedEventPort carePlanConfirmedEventPort;

    private CarePlanOutboxRelayFacade carePlanOutboxRelayFacade;

    @BeforeEach
    void setUp() {
        carePlanOutboxRelayFacade = new CarePlanOutboxRelayFacade(
                carePlanOutboxQueryService,
                carePlanOutboxCommandService,
                carePlanCompletionEventPayloadSerializer,
                carePlanCompletedEventPort,
                carePlanConfirmedEventPayloadSerializer,
                carePlanConfirmedEventPort
        );
    }

    @Test
    @DisplayName("CARE_PLAN_COMPLETED Outbox 이벤트 발행 성공 시 SENT 상태 변경을 요청")
    void relay_completed_success() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                completedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        CarePlanCompletionEvent event = new CarePlanCompletionEvent(
                UUID.randomUUID(),
                carePlanId,
                UUID.randomUUID(),
                Instant.now()
        );

        given(carePlanOutboxQueryService.findPending(100)).willReturn(List.of(pendingEvent));
        given(carePlanCompletionEventPayloadSerializer.deserialize("{}")).willReturn(event);

        carePlanOutboxRelayFacade.relay();

        verify(carePlanCompletedEventPort).publish(event);
        verify(carePlanConfirmedEventPort, never()).publish(any());
        verify(carePlanOutboxCommandService).markSent(completedOutboxEventId);
        verify(carePlanOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    @DisplayName("CARE_PLAN_COMPLETED Outbox 이벤트 발행 실패 시 실패 횟수 기록을 요청")
    void relay_completed_failure() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                completedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        CarePlanCompletionEvent event = new CarePlanCompletionEvent(
                UUID.randomUUID(),
                carePlanId,
                UUID.randomUUID(),
                Instant.now()
        );

        given(carePlanOutboxQueryService.findPending(100)).willReturn(List.of(pendingEvent));
        given(carePlanCompletionEventPayloadSerializer.deserialize("{}")).willReturn(event);

        doThrow(new RuntimeException("RabbitMQ 발행 실패"))
                .when(carePlanCompletedEventPort)
                .publish(event);

        carePlanOutboxRelayFacade.relay();

        verify(carePlanCompletedEventPort).publish(event);
        verify(carePlanOutboxCommandService, never()).markSent(any());
        verify(carePlanOutboxCommandService)
                .recordFailure(
                        completedOutboxEventId,
                        "RabbitMQ 발행 실패"
                );
    }

    @Test
    @DisplayName("CARE_PLAN_CONFIRMED Outbox 이벤트는 CarePlanConfirmedEvent로 역직렬화되어 발행되고 SENT로 변경")
    void relay_confirmed_success() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                confirmedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_CONFIRMED,
                "{}"
        );

        CarePlanConfirmedEvent event = new CarePlanConfirmedEvent(
                carePlanId,
                UUID.randomUUID(),
                List.of()
        );

        given(carePlanOutboxQueryService.findPending(100)).willReturn(List.of(pendingEvent));
        given(carePlanConfirmedEventPayloadSerializer.deserialize("{}")).willReturn(event);

        carePlanOutboxRelayFacade.relay();

        verify(carePlanConfirmedEventPort).publish(event);
        verify(carePlanCompletedEventPort, never()).publish(any());
        verify(carePlanOutboxCommandService).markSent(confirmedOutboxEventId);
        verify(carePlanOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    @DisplayName("CARE_PLAN_CONFIRMED Outbox 이벤트 발행 실패 시에도 기존과 동일한 실패 기록 정책이 적용됨")
    void relay_confirmed_failure() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                confirmedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_CONFIRMED,
                "{}"
        );

        CarePlanConfirmedEvent event = new CarePlanConfirmedEvent(
                carePlanId,
                UUID.randomUUID(),
                List.of()
        );

        given(carePlanOutboxQueryService.findPending(100)).willReturn(List.of(pendingEvent));
        given(carePlanConfirmedEventPayloadSerializer.deserialize("{}")).willReturn(event);

        doThrow(new RuntimeException("RabbitMQ 발행 실패"))
                .when(carePlanConfirmedEventPort)
                .publish(event);

        carePlanOutboxRelayFacade.relay();

        verify(carePlanConfirmedEventPort).publish(event);
        verify(carePlanOutboxCommandService, never()).markSent(any());
        verify(carePlanOutboxCommandService)
                .recordFailure(
                        confirmedOutboxEventId,
                        "RabbitMQ 발행 실패"
                );
    }

    @Test
    @DisplayName("같은 배치에 CONFIRMED / COMPLETED가 섞여 있으면 각각 올바른 타입으로 역직렬화되고 올바른 Port로 발행")
    void relay_mixedBatch_dispatchesEachByEventType() {
        CarePlanOutboxEventResult confirmedPending = new CarePlanOutboxEventResult(
                confirmedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_CONFIRMED,
                "confirmed-payload"
        );

        CarePlanOutboxEventResult completedPending = new CarePlanOutboxEventResult(
                completedOutboxEventId,
                carePlanId,
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "completed-payload"
        );

        CarePlanConfirmedEvent confirmedEvent = new CarePlanConfirmedEvent(
                carePlanId,
                UUID.randomUUID(),
                List.of()
        );

        CarePlanCompletionEvent completedEvent = new CarePlanCompletionEvent(
                UUID.randomUUID(),
                carePlanId,
                UUID.randomUUID(),
                Instant.now()
        );

        given(carePlanOutboxQueryService.findPending(100))
                .willReturn(List.of(confirmedPending, completedPending));
        given(carePlanConfirmedEventPayloadSerializer.deserialize("confirmed-payload"))
                .willReturn(confirmedEvent);
        given(carePlanCompletionEventPayloadSerializer.deserialize("completed-payload"))
                .willReturn(completedEvent);

        carePlanOutboxRelayFacade.relay();

        verify(carePlanConfirmedEventPort).publish(confirmedEvent);
        verify(carePlanCompletedEventPort).publish(completedEvent);
        verify(carePlanCompletionEventPayloadSerializer, never()).deserialize("confirmed-payload");
        verify(carePlanConfirmedEventPayloadSerializer, never()).deserialize("completed-payload");
        verify(carePlanOutboxCommandService).markSent(confirmedOutboxEventId);
        verify(carePlanOutboxCommandService).markSent(completedOutboxEventId);
    }
}
