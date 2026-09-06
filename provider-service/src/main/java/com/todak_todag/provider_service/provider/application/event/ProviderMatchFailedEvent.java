package com.todak_todag.provider_service.provider.application.event;

import com.todak_todag.provider_service.global.common.TimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderMatchFailedEvent(
        UUID carePlanId,
        UUID regionId,
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate date,
        TimeSlot preferredTimeSlot,
        String failureReason,
        Instant failedAt
) {

    public static final String NO_AVAILABLE_PROVIDER = "NO_AVAILABLE_PROVIDER";
}