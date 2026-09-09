package com.todak_todag.social_worker_service.matching.application.support.task;

import java.util.UUID;

public record MatchingTask(
        UUID taskId,
        MatchingTaskStatus status,
        UUID matchingResultId
) {

    public static MatchingTask pending(
            UUID taskId
    ) {
        return new MatchingTask(
                taskId,
                MatchingTaskStatus.PENDING,
                null
        );
    }

    public MatchingTask processing() {
        return new MatchingTask(
                taskId,
                MatchingTaskStatus.PROCESSING,
                matchingResultId
        );
    }

    public MatchingTask completed(
            UUID matchingResultId
    ) {
        return new MatchingTask(
                taskId,
                MatchingTaskStatus.COMPLETED,
                matchingResultId
        );
    }

    public MatchingTask failed(
            UUID matchingResultId
    ) {
        return new MatchingTask(
                taskId,
                MatchingTaskStatus.FAILED,
                matchingResultId
        );
    }
}