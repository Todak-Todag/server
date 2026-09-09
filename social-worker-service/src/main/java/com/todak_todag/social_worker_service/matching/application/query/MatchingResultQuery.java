package com.todak_todag.social_worker_service.matching.application.query;

import com.todak_todag.social_worker_service.global.common.UserRole;

import java.util.UUID;

public record MatchingResultQuery(
        UUID matchingResultId,
        UUID requesterId,
        UserRole requesterRole
) {
}