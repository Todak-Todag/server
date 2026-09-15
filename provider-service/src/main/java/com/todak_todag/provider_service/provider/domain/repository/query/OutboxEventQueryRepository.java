package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventQueryRepository {

    List<ProviderOutboxEvent> findPending(int limit);

    Optional<ProviderOutboxEvent> findById(UUID outboxEventId);

    // 중복 수신 판별용: 희망 일정별로 이미 적재된 매칭 결과
    List<ProviderOutboxEvent> findAllByAggregateIdIn(Collection<UUID> aggregateIds);

    List<ProviderOutboxEvent> findAllByAggregateIdAndCreatedAtAfter(UUID aggregateId, Instant createdAt);
}