package com.todak_todag.social_worker_service.matching.presentation.response;

import com.todak_todag.social_worker_service.matching.application.result.MatchingStatusChangeResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;

import java.util.UUID;

public record MatchingStatusChangeResponse(
        UUID matchingResultId,
        MatchingStatus status
) {

    public static MatchingStatusChangeResponse from(
            MatchingStatusChangeResult result
    ) {

        return new MatchingStatusChangeResponse(
                result.matchingResultId(),
                result.status()
        );
    }
}