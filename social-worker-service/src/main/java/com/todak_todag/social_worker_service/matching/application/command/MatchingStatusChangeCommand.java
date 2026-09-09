package com.todak_todag.social_worker_service.matching.application.command;

import com.todak_todag.social_worker_service.global.common.UserRole;

import java.util.UUID;

public record MatchingStatusChangeCommand(
        UUID matchingResultId,
        String status,
        UUID requesterId,
        UserRole requesterRole
) {
}