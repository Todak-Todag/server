package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CarePlanServiceFindResult(
        UUID planServiceId,
        UUID provideServiceId,
        String provideServiceName,
        String provideServiceContent,
        List<PreferenceSummary> preferences,
        Instant createdAt
) {
    public record PreferenceSummary(
            UUID servicePreferenceId,
            LocalDate preferredDate,
            PreferredTimeSlot preferredTimeSlot
    ) {
    }
}
