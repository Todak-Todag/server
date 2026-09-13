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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanOutboxRelayFacade {
    private static final int BATCH_SIZE = 100;

    // PROCESSING으로 선점된 채 이만큼 갱신되지 않으면
    // 선점한 인스턴스가 죽은 것으로 보고 PENDING으로 되돌린다.
    private static final Duration STUCK_PROCESSING_THRESHOLD = Duration.ofMinutes(1);

    private final CarePlanOutboxQueryService carePlanOutboxQueryService;
    private final CarePlanOutboxCommandService carePlanOutboxCommandService;
    private final CarePlanCompletionEventPayloadSerializer carePlanCompletionEventPayloadSerializer;
    private final CarePlanCompletedEventPort carePlanCompletedEventPort;
    private final CarePlanConfirmedEventPayloadSerializer carePlanConfirmedEventPayloadSerializer;
    private final CarePlanConfirmedEventPort carePlanConfirmedEventPort;

    public void relay() {
        reclaimStuckProcessing();

        List<CarePlanOutboxEventResult> pendingEvents =
                carePlanOutboxQueryService.findPending(BATCH_SIZE);

        for (CarePlanOutboxEventResult pendingEvent : pendingEvents) {
            relayOne(pendingEvent);
        }
    }

    // 선점(PROCESSING) 이후 markSent/recordFailure 없이 죽은 인스턴스에
    // 방치된 이벤트를 PENDING으로 되돌려 다음 시도 대상에 포함시킨다.
    private void reclaimStuckProcessing() {
        Instant threshold = Instant.now().minus(STUCK_PROCESSING_THRESHOLD);

        List<CarePlanOutboxEventResult> stuckEvents =
                carePlanOutboxQueryService.findStuckProcessing(threshold, BATCH_SIZE);

        for (CarePlanOutboxEventResult stuckEvent : stuckEvents) {
            try {
                carePlanOutboxCommandService.revertStuckProcessing(
                        stuckEvent.outboxEventId(),
                        threshold
                );
            } catch (ObjectOptimisticLockingFailureException e) {
                // 다른 인스턴스가 그 사이 이미 복구했거나 처리를 끝냈으므로 무시한다.
            }
        }
    }

    private void relayOne(
            CarePlanOutboxEventResult pendingEvent
    ) {
        if (!tryClaim(pendingEvent.outboxEventId())) {
            return;
        }

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

    // PENDING -> PROCESSING 선점을 시도한다.
    // 다른 인스턴스가 이미 선점했다면(낙관적 락 충돌 포함) false를 반환해 건너뛴다.
    private boolean tryClaim(UUID outboxEventId) {
        try {
            return carePlanOutboxCommandService.claim(outboxEventId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info(
                    "[CarePlan] 다른 인스턴스가 이미 선점한 Outbox 이벤트 outboxEventId={}",
                    outboxEventId
            );

            return false;
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
