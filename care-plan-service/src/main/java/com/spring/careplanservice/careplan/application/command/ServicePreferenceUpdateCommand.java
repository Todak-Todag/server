package com.spring.careplanservice.careplan.application.command;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceUpdateCommand(
        UUID userId,
        UUID servicePreferenceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot
) {
}
