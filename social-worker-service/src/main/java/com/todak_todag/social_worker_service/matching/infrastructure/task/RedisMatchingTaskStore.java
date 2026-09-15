package com.todak_todag.social_worker_service.matching.infrastructure.task;

import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisMatchingTaskStore
        implements MatchingTaskStore {

    private static final String KEY_PREFIX =
            "social-worker:matching-task:";

    private static final String DELIMITER = "\\|";
    private static final String SERIALIZATION_DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisMatchingTaskStore(
            StringRedisTemplate redisTemplate,
            @Value("${matching.task.ttl:1h}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public void save(
            MatchingTask task
    ) {
        redisTemplate
                .opsForValue()
                .set(
                        createKey(task.taskId()),
                        serialize(task),
                        ttl
                );
    }

    @Override
    public Optional<MatchingTask> findByTaskId(
            UUID taskId
    ) {
        String value =
                redisTemplate
                        .opsForValue()
                        .get(
                                createKey(taskId)
                        );

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(
                deserialize(value)
        );
    }

    private String createKey(
            UUID taskId
    ) {
        return KEY_PREFIX + taskId;
    }

    private String serialize(
            MatchingTask task
    ) {
        String matchingResultId =
                task.matchingResultId() == null
                        ? ""
                        : task.matchingResultId().toString();

        return String.join(
                SERIALIZATION_DELIMITER,
                task.taskId().toString(),
                task.patientId().toString(),
                task.status().name(),
                matchingResultId
        );
    }

    private MatchingTask deserialize(
            String value
    ) {
        String[] values =
                value.split(
                        DELIMITER,
                        -1
                );

        if (values.length != 4) {
            throw new IllegalStateException(
                    "Redis MatchingTask 데이터 형식이 올바르지 않습니다."
            );
        }

        UUID matchingResultId =
                values[3].isBlank()
                        ? null
                        : UUID.fromString(values[3]);

        return new MatchingTask(
                UUID.fromString(values[0]),
                UUID.fromString(values[1]),
                MatchingTaskStatus.valueOf(values[2]),
                matchingResultId
        );
    }
}