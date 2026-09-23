package com.todak_todag.schedule_service.schedule.application.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// ProviderMatched 이벤트 페이로드 (수신)
public record ProviderMatchedEvent(
        UUID carePlanId,
        UUID regionId,
        UUID provideServiceId,
        UUID servicePreferenceId,
        UUID serviceOfferingId,
        LocalDate date,
        LocalDateTime startedAt,
        Instant matchedAt
) {
}
