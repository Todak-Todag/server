package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceFindResult(
        UUID servicePreferenceId,
        UUID planServiceId,
        UUID provideServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot,
        Instant createdAt
) {
}
