package com.todak_todag.schedule_service.schedule.application.event;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;

import java.util.UUID;

// CarePlanCompleted 이벤트 페이로드 (발행)
public record CarePlanCompletedEvent(
        UUID carePlanId,
        UUID serviceResultId,
        ScheduleStatus status
) {
}
