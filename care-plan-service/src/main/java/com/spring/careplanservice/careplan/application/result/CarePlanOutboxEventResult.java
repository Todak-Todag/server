package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;

import java.util.UUID;

public record CarePlanOutboxEventResult(
        UUID outboxEventId,
        UUID aggregateId,
        CarePlanOutboxEventType eventType,
        String payload
) {

    public static CarePlanOutboxEventResult from(
            CarePlanOutboxEvent outboxEvent
    ) {
        return new CarePlanOutboxEventResult(
                outboxEvent.getId(),
                outboxEvent.getAggregateId(),
                outboxEvent.getEventType(),
                outboxEvent.getPayload()
        );
    }
}