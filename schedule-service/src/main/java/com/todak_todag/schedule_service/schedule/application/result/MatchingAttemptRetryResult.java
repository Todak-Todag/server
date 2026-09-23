package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;

import java.time.LocalDate;
import java.util.UUID;

public record MatchingAttemptRetryResult(
        UUID matchingAttemptId,
        UUID servicePreferenceId,
        LocalDate date,
        PreferredTimeSlot preferredTimeSlot
) {
}
