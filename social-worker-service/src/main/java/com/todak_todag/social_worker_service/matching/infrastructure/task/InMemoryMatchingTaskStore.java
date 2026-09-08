package com.todak_todag.social_worker_service.matching.infrastructure.task;

import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMatchingTaskStore
        implements MatchingTaskStore {

    private final Map<UUID, MatchingTask> store =
            new ConcurrentHashMap<>();

    @Override
    public void save(
            MatchingTask task
    ) {
        store.put(
                task.taskId(),
                task
        );
    }

    @Override
    public Optional<MatchingTask> findByTaskId(
            UUID taskId
    ) {
        return Optional.ofNullable(
                store.get(taskId)
        );
    }
}