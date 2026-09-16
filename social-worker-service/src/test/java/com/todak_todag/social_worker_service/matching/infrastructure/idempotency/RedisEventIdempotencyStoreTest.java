package com.todak_todag.social_worker_service.matching.infrastructure.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RedisEventIdempotencyStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisEventIdempotencyStore store;

    @BeforeEach
    void setUp() {

        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(
                redisTemplate.opsForValue()
        ).thenReturn(
                valueOperations
        );

        store =
                new RedisEventIdempotencyStore(
                        redisTemplate,
                        Duration.ofHours(24)
                );
    }

    @Test
    @DisplayName("eventId 키가 없으면 처리권 획득에 성공한다")
    void tryAcquireSucceedsWhenKeyDoesNotExist() {

        UUID eventId = UUID.randomUUID();

        when(
                valueOperations.setIfAbsent(
                        anyString(),
                        eq("1"),
                        eq(Duration.ofHours(24))
                )
        ).thenReturn(true);

        boolean acquired =
                store.tryAcquire(
                        eventId
                );

        assertTrue(
                acquired
        );
    }

    @Test
    @DisplayName("eventId 키가 이미 존재하면 처리권 획득에 실패한다")
    void tryAcquireFailsWhenKeyAlreadyExists() {

        UUID eventId = UUID.randomUUID();

        when(
                valueOperations.setIfAbsent(
                        anyString(),
                        eq("1"),
                        eq(Duration.ofHours(24))
                )
        ).thenReturn(false);

        boolean acquired =
                store.tryAcquire(
                        eventId
                );

        assertFalse(
                acquired
        );
    }

    @Test
    @DisplayName("이벤트 처리 실패 시 eventId 키를 삭제한다")
    void releaseDeletesKey() {

        UUID eventId = UUID.randomUUID();

        store.release(
                eventId
        );

        verify(
                redisTemplate,
                times(1)
        ).delete(
                "social-worker:event-idempotency:" + eventId
        );
    }
}