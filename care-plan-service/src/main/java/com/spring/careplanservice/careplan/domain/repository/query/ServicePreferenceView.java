package com.spring.careplanservice.careplan.domain.repository.query;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceView(
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot,
        Instant createdAt
) {
}
