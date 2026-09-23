package com.todak_todag.social_worker_service.matching.application.result;

import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;

import java.util.UUID;

public record MatchingTaskStatusResult(
        UUID taskId,
        MatchingTaskStatus taskStatus,
        UUID matchingResultId
) {

    public static MatchingTaskStatusResult from(
            MatchingTask task
    ) {
        return new MatchingTaskStatusResult(
                task.taskId(),
                task.status(),
                task.matchingResultId()
        );
    }
}