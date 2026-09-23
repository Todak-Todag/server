package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MatchingAttemptRetryRequest(
        @NotNull
        LocalDate date,

        PreferredTimeSlot preferredTimeSlot
) {

    public MatchingAttemptRetryCommand toCommand(UUID matchingAttemptId, UUID requesterId) {
        return new MatchingAttemptRetryCommand(matchingAttemptId, date, preferredTimeSlot, requesterId);
    }
}
