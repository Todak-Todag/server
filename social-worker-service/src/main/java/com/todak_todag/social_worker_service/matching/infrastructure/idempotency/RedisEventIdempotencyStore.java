package com.todak_todag.social_worker_service.matching.infrastructure.idempotency;

import com.todak_todag.social_worker_service.matching.application.support.idempotency.EventIdempotencyStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisEventIdempotencyStore
        implements EventIdempotencyStore {

    private static final String KEY_PREFIX =
            "social-worker:event-idempotency:";

    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisEventIdempotencyStore(
            StringRedisTemplate redisTemplate,
            @Value("${event.idempotency.ttl:24h}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public boolean tryAcquire(
            UUID eventId
    ) {

        Boolean acquired =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                createKey(eventId),
                                VALUE,
                                ttl
                        );

        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(
            UUID eventId
    ) {

        redisTemplate.delete(
                createKey(eventId)
        );
    }

    private String createKey(
            UUID eventId
    ) {
        return KEY_PREFIX + eventId;
    }
}