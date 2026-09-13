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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanOutboxRelayFacade {
    private static final int BATCH_SIZE = 100;

    private final CarePlanOutboxQueryService carePlanOutboxQueryService;
    private final CarePlanOutboxCommandService carePlanOutboxCommandService;
    private final CarePlanCompletionEventPayloadSerializer carePlanCompletionEventPayloadSerializer;
    private final CarePlanCompletedEventPort carePlanCompletedEventPort;
    private final CarePlanConfirmedEventPayloadSerializer carePlanConfirmedEventPayloadSerializer;
    private final CarePlanConfirmedEventPort carePlanConfirmedEventPort;

    public void relay() {
        List<CarePlanOutboxEventResult> pendingEvents =
                carePlanOutboxQueryService.findPending(BATCH_SIZE);

        for (CarePlanOutboxEventResult pendingEvent : pendingEvents) {
            relayOne(pendingEvent);
        }
    }

    private void relayOne(
            CarePlanOutboxEventResult pendingEvent
    ) {
        try {
            publish(pendingEvent);

            carePlanOutboxCommandService.markSent(
                    pendingEvent.outboxEventId()
            );

        } catch (Exception e) {
            log.error(
                    "[CarePlan] Outbox 이벤트 발행 실패 outboxEventId={}",
                    pendingEvent.outboxEventId(),
                    e
            );

            carePlanOutboxCommandService.recordFailure(
                    pendingEvent.outboxEventId(),
                    e.getMessage()
            );
        }
    }

    // Outbox row의 eventType에 따라 알맞은 이벤트 타입으로 역직렬화하고
    // 그 이벤트 전용 Exchange / Routing Key로 발행한다.
    private void publish(
            CarePlanOutboxEventResult pendingEvent
    ) {
        switch (pendingEvent.eventType()) {
            case CARE_PLAN_CONFIRMED -> {
                CarePlanConfirmedEvent event =
                        carePlanConfirmedEventPayloadSerializer.deserialize(
                                pendingEvent.payload()
                        );

                carePlanConfirmedEventPort.publish(event);
            }
            case CARE_PLAN_COMPLETED -> {
                CarePlanCompletionEvent event =
                        carePlanCompletionEventPayloadSerializer.deserialize(
                                pendingEvent.payload()
                        );

                carePlanCompletedEventPort.publish(event);
            }
        }
    }
}
