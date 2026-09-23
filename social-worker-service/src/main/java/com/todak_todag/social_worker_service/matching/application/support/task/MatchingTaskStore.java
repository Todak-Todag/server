package com.todak_todag.social_worker_service.matching.application.support.task;

import java.util.Optional;
import java.util.UUID;

public interface MatchingTaskStore {

    void save(
            MatchingTask task
    );

    Optional<MatchingTask> findByTaskId(
            UUID taskId
    );
}