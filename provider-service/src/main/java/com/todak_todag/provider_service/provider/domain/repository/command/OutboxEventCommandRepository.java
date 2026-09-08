package com.todak_todag.provider_service.provider.domain.repository.command;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;

public interface OutboxEventCommandRepository {

    ProviderOutboxEvent save(ProviderOutboxEvent outboxEvent);
}