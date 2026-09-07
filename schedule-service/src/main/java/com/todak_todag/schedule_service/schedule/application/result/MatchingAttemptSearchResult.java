package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MatchingAttemptSearchResult(
        UUID matchingAttemptId,
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate date,
        PreferredTimeSlot preferredTimeSlot,
        MatchingAttemptStatus status,
        String failureReason,
        Instant failedAt,
        Instant matchedAt
) {

    public static MatchingAttemptSearchResult from(ServiceMatchingAttempt attempt) {
        return new MatchingAttemptSearchResult(
                attempt.getId(),
                attempt.getServicePreferenceId(),
                attempt.getProvideServiceId(),
                attempt.getDate(),
                attempt.getPreferredTimeSlot(),
                attempt.getStatus(),
                attempt.getFailureReason(),
                attempt.getFailedAt(),
                attempt.getMatchedAt()
        );
    }
}
