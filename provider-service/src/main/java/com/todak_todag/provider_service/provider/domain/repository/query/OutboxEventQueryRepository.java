package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventQueryRepository {

    List<ProviderOutboxEvent> findPending(int limit);

    Optional<ProviderOutboxEvent> findById(UUID outboxEventId);
}