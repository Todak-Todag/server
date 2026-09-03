package com.spring.careplanservice.careplan.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CarePlanCompletedEvent(
        UUID carePlanId,
        UUID patientId,
        LocalDateTime completedAt
) {
}
