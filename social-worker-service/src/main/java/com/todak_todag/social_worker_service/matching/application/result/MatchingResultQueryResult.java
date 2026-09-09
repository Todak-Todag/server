package com.todak_todag.social_worker_service.matching.application.result;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;

import java.time.Instant;
import java.util.UUID;

public record MatchingResultQueryResult(
        UUID matchingResultId,
        UUID patientId,
        UUID socialWorkerId,
        MatchingStatus status,
        Instant requestedAt,
        Instant assignedAt
) {

    public static MatchingResultQueryResult from(
            SocialWorkerMatchingResult matchingResult
    ) {

        return new MatchingResultQueryResult(
                matchingResult.getMatchingResultId(),
                matchingResult.getPatientId(),
                matchingResult.getSocialWorkerId(),
                matchingResult.getStatus(),
                matchingResult.getRequestedAt(),
                matchingResult.getAssignedAt()
        );
    }
}