package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEventPayloadSerializer;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
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
            CarePlanCompletionEvent event =
                    carePlanCompletionEventPayloadSerializer.deserialize(
                            pendingEvent.payload()
                    );

            carePlanCompletedEventPort.publish(event);

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
}
