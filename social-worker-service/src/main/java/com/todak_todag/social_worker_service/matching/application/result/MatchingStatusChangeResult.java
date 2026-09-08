package com.todak_todag.social_worker_service.matching.application.result;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;

import java.util.UUID;

public record MatchingStatusChangeResult(
        UUID matchingResultId,
        MatchingStatus status
) {
}