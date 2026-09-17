package com.todak_todag.social_worker_service.matching.application.support.idempotency;

import java.util.UUID;

public interface EventIdempotencyStore {

    boolean tryAcquire(
            UUID eventId
    );

    void release(
            UUID eventId
    );
}