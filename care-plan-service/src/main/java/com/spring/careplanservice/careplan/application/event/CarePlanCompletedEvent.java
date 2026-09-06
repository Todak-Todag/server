package com.spring.careplanservice.careplan.application.event;

import java.util.UUID;

public record CarePlanCompletedEvent(
        UUID serviceResultId,
        ScheduleStatus status
) {
}
