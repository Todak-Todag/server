package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;

import java.time.LocalDate;
import java.util.UUID;

public record MatchingAttemptRetryResponse(
        UUID matchingAttemptId,
        UUID servicePreferenceId,
        LocalDate date,
        String preferredTimeSlot
) {

    public static MatchingAttemptRetryResponse from(MatchingAttemptRetryResult result) {
        return new MatchingAttemptRetryResponse(
                result.matchingAttemptId(),
                result.servicePreferenceId(),
                result.date(),
                result.preferredTimeSlot() == null ? null : result.preferredTimeSlot().name()
        );
    }
}
