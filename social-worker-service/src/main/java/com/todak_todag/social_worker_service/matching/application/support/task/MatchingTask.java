package com.todak_todag.social_worker_service.matching.application.support.task;

import java.util.UUID;

public record MatchingTask(
        UUID taskId,
        UUID patientId,
        MatchingTaskStatus status,
        UUID matchingResultId
) {

    public static MatchingTask pending(
            UUID taskId,
            UUID patientId
    ) {
        return new MatchingTask(
                taskId,
                patientId,
                MatchingTaskStatus.PENDING,
                null
        );
    }

    public MatchingTask processing() {
        return new MatchingTask(
                taskId,
                patientId,
                MatchingTaskStatus.PROCESSING,
                matchingResultId
        );
    }

    public MatchingTask completed(
            UUID matchingResultId
    ) {
        return new MatchingTask(
                taskId,
                patientId,
                MatchingTaskStatus.COMPLETED,
                matchingResultId
        );
    }

    public MatchingTask failed(
            UUID matchingResultId
    ) {
        return new MatchingTask(
                taskId,
                patientId,
                MatchingTaskStatus.FAILED,
                matchingResultId
        );
    }
}