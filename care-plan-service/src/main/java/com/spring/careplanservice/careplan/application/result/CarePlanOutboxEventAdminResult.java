package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;

import java.time.Instant;
import java.util.UUID;

// 운영자가 FAILED Outbox 이벤트를 확인/재처리할 때 필요한 정보를 담는다.
// Relay 내부 발행 로직이 쓰는 CarePlanOutboxEventResult와는 용도가 달라 별도로 둔다.
public record CarePlanOutboxEventAdminResult(
        UUID outboxEventId,
        UUID aggregateId,
        CarePlanOutboxEventType eventType,
        CarePlanOutboxEventStatus status,
        int retryCount,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt
) {

    public static CarePlanOutboxEventAdminResult from(
            CarePlanOutboxEvent outboxEvent
    ) {
        return new CarePlanOutboxEventAdminResult(
                outboxEvent.getId(),
                outboxEvent.getAggregateId(),
                outboxEvent.getEventType(),
                outboxEvent.getStatus(),
                outboxEvent.getRetryCount(),
                outboxEvent.getLastErrorMessage(),
                outboxEvent.getCreatedAt(),
                outboxEvent.getUpdatedAt()
        );
    }
}
