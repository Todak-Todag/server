package com.todak_todag.schedule_service.schedule.application.event;

import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// ProviderMatchFailed 이벤트 페이로드 (수신)
public record ProviderMatchFailedEvent(
        UUID carePlanId,
        UUID regionId,
        UUID provideServiceId,
        UUID servicePreferenceId,
        LocalDate date,
        PreferredTimeSlot preferredTimeSlot,
        String failureReason,
        Instant failedAt
) {
}
