package com.spring.careplanservice.careplan.application.command;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceCreateCommand(
        UUID userId,
        UUID planServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot
) {
}