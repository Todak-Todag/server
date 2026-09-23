package com.todak_todag.social_worker_service.matching.presentation.response;

import com.todak_todag.social_worker_service.matching.application.result.MatchingTaskStatusResult;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;

import java.util.UUID;

public record MatchingTaskStatusResponse(
        UUID taskId,
        MatchingTaskStatus taskStatus,
        UUID matchingResultId
) {

    public static MatchingTaskStatusResponse from(
            MatchingTaskStatusResult result
    ) {
        return new MatchingTaskStatusResponse(
                result.taskId(),
                result.taskStatus(),
                result.matchingResultId()
        );
    }
}