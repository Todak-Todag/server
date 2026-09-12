package com.todak_todag.social_worker_service.matching.infrastructure.task;

import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTask;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisMatchingTaskStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisMatchingTaskStore store;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        store = new RedisMatchingTaskStore(
                redisTemplate,
                Duration.ofHours(1)
        );
    }

    @Test
    @DisplayName("MatchingTask를 Redis에 TTL과 함께 저장한다")
    void saveTaskWithTtl() {
        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        MatchingTask task =
                MatchingTask.pending(
                        taskId,
                        patientId
                );

        store.save(task);

        verify(valueOperations).set(
                eq("social-worker:matching-task:" + taskId),
                anyString(),
                eq(Duration.ofHours(1))
        );
    }

    @Test
    @DisplayName("Redis의 COMPLETED MatchingTask를 조회한다")
    void findCompletedTask() {
        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();

        MatchingTask task =
                MatchingTask
                        .pending(taskId, patientId)
                        .completed(resultId);

        store.save(task);

        ArgumentCaptor<String> valueCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(valueOperations).set(
                anyString(),
                valueCaptor.capture(),
                any(Duration.class)
        );

        when(
                valueOperations.get(
                        "social-worker:matching-task:" + taskId
                )
        ).thenReturn(valueCaptor.getValue());

        MatchingTask result =
                store.findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(taskId, result.taskId());
        assertEquals(patientId, result.patientId());
        assertEquals(
                MatchingTaskStatus.COMPLETED,
                result.status()
        );
        assertEquals(
                resultId,
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("Redis에 taskId가 없으면 Optional.empty를 반환한다")
    void taskNotFound() {
        UUID taskId = UUID.randomUUID();

        when(
                valueOperations.get(
                        "social-worker:matching-task:" + taskId
                )
        ).thenReturn(null);

        Optional<MatchingTask> result =
                store.findByTaskId(taskId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("FAILED Task는 matchingResultId 없이 역직렬화한다")
    void deserializeFailedTask() {
        UUID taskId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(
                valueOperations.get(
                        "social-worker:matching-task:" + taskId
                )
        ).thenReturn(
                taskId + "|" + patientId + "|FAILED|"
        );

        MatchingTask result =
                store.findByTaskId(taskId)
                        .orElseThrow();

        assertEquals(
                MatchingTaskStatus.FAILED,
                result.status()
        );
        assertEquals(
                patientId,
                result.patientId()
        );
        assertNull(
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("Redis 데이터 형식이 잘못되면 예외가 발생한다")
    void invalidRedisValueThrowsException() {
        UUID taskId = UUID.randomUUID();

        when(
                valueOperations.get(
                        "social-worker:matching-task:" + taskId
                )
        ).thenReturn("invalid-value");

        assertThrows(
                IllegalStateException.class,
                () -> store.findByTaskId(taskId)
        );
    }
}