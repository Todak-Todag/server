package com.spring.careplanservice.careplan.application.event;

import java.util.UUID;

public record CarePlanCompletedEvent(
        UUID carePlanId,
        UUID serviceResultId,
        ScheduleStatus status
) {
}
