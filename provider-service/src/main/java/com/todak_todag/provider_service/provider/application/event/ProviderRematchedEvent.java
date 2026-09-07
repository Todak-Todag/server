package com.todak_todag.provider_service.provider.application.event;

import com.todak_todag.provider_service.global.common.TimeSlot;

import java.time.LocalDate;
import java.util.UUID;

public record ProviderRematchedEvent(
        UUID carePlanId,
        UUID regionId,
        UUID provideServiceId,
        UUID servicePreferenceId,
        LocalDate date,
        TimeSlot preferredTimeSlot
) {
}