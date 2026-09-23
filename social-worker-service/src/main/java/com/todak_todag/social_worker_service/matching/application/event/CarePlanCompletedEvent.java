package com.todak_todag.social_worker_service.matching.application.event;

import java.time.Instant;
import java.util.UUID;

public record CarePlanCompletedEvent(
        UUID eventId,
        UUID carePlanId,
        UUID patientId,
        Instant completedAt
) {
}