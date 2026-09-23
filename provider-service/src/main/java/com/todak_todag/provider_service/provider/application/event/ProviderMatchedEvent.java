package com.todak_todag.provider_service.provider.application.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProviderMatchedEvent(
        UUID carePlanId,
        UUID regionId,
        UUID servicePreferenceId,
        UUID provideServiceId,
        UUID serviceOfferingId,
        LocalDate date,
        LocalDateTime startedAt,
        Instant matchedAt
) {
}