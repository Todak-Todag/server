package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanOutboxEventAdminResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;

import java.time.Instant;
import java.util.UUID;

public record CarePlanOutboxEventAdminResponse(
        UUID outboxEventId,
        UUID aggregateId,
        CarePlanOutboxEventType eventType,
        CarePlanOutboxEventStatus status,
        int retryCount,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt
) {

    public static CarePlanOutboxEventAdminResponse from(
            CarePlanOutboxEventAdminResult result
    ) {
        return new CarePlanOutboxEventAdminResponse(
                result.outboxEventId(),
                result.aggregateId(),
                result.eventType(),
                result.status(),
                result.retryCount(),
                result.lastErrorMessage(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
