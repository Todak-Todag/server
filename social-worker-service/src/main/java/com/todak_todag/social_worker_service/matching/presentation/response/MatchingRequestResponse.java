package com.todak_todag.social_worker_service.matching.presentation.response;

import java.util.UUID;

public record MatchingRequestResponse(
        UUID taskId
) {

    public static MatchingRequestResponse from(
            UUID taskId
    ) {
        return new MatchingRequestResponse(
                taskId
        );
    }
}