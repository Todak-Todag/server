package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;

import java.util.UUID;

// 릴레이(ScheduleOutboxRelayFacade)가 아웃박스 이벤트 1건을 처리하는 데 필요한 최소 정보
public record ScheduleOutboxEventResult(
        UUID outboxEventId,
        String eventType,
        UUID aggregateId,
        String payload
) {

    public static ScheduleOutboxEventResult from(ScheduleOutboxEvent scheduleOutboxEvent) {
        return new ScheduleOutboxEventResult(
                scheduleOutboxEvent.getId(),
                scheduleOutboxEvent.getEventType(),
                scheduleOutboxEvent.getAggregateId(),
                scheduleOutboxEvent.getPayload()
        );
    }
}
