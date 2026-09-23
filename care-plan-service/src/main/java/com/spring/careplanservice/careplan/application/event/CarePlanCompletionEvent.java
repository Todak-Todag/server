package com.spring.careplanservice.careplan.application.event;

import java.time.Instant;
import java.util.UUID;

public record CarePlanCompletionEvent(
        // CarePlan → SocialWorker
        UUID eventId,
        UUID carePlanId,
        UUID patientId,
        Instant completedAt
) {
}
