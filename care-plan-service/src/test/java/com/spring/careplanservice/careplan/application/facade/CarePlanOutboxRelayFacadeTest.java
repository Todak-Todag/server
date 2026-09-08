package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEventPayloadSerializer;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import com.spring.careplanservice.careplan.application.result.CarePlanOutboxEventResult;
import com.spring.careplanservice.careplan.application.service.command.CarePlanOutboxCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanOutboxQueryService;
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
    UUID outboxEventId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();

    @Mock
    private CarePlanOutboxQueryService carePlanOutboxQueryService;

    @Mock
    private CarePlanOutboxCommandService carePlanOutboxCommandService;

    @Mock
    private CarePlanCompletionEventPayloadSerializer carePlanCompletionEventPayloadSerializer;

    @Mock
    private CarePlanCompletedEventPort carePlanCompletedEventPort;

    private CarePlanOutboxRelayFacade carePlanOutboxRelayFacade;

    @BeforeEach
    void setUp() {
        carePlanOutboxRelayFacade = new CarePlanOutboxRelayFacade(
                carePlanOutboxQueryService,
                carePlanOutboxCommandService,
                carePlanCompletionEventPayloadSerializer,
                carePlanCompletedEventPort
        );
    }

    @Test
    @DisplayName("Outbox 이벤트 발행 성공 시 SENT 상태 변경을 요청")
    void relay_success() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                outboxEventId,
                carePlanId,
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
        verify(carePlanOutboxCommandService).markSent(outboxEventId);
        verify(carePlanOutboxCommandService, never()).recordFailure(any(), anyString());
    }

    @Test
    @DisplayName("Outbox 이벤트 발행 실패 시 실패 횟수 기록을 요청")
    void relay_failure() {
        CarePlanOutboxEventResult pendingEvent = new CarePlanOutboxEventResult(
                outboxEventId,
                carePlanId,
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
                        outboxEventId,
                        "RabbitMQ 발행 실패"
                );
    }
}