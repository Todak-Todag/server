package com.todak_todag.social_worker_service.matching.presentation.response;

import com.todak_todag.social_worker_service.matching.application.result.MatchingResultQueryResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchingResultResponse(
        UUID matchingResultId,
        UUID patientId,
        UUID socialWorkerId,
        MatchingStatus status,
        Instant requestedAt,
        Instant assignedAt
) {

    public static MatchingResultResponse from(
            MatchingResultQueryResult result
    ) {

        return new MatchingResultResponse(
                result.matchingResultId(),
                result.patientId(),
                result.socialWorkerId(),
                result.status(),
                result.requestedAt(),
                result.assignedAt()
        );
    }
}