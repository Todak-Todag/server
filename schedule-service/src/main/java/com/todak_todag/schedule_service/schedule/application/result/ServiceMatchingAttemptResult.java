package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.util.UUID;

public record ServiceMatchingAttemptResult(
        UUID matchingAttemptId,
        UUID servicePreferenceId
) {

    public static ServiceMatchingAttemptResult from(ServiceMatchingAttempt attempt) {
        return new ServiceMatchingAttemptResult(
                attempt.getId(),
                attempt.getServicePreferenceId()
        );
    }
}
