package com.todak_todag.provider_service.provider.infrastructure.persistence.command;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.command.OutboxEventCommandRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxEventCommandRepositoryImpl implements OutboxEventCommandRepository {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;

    @Override
    public ProviderOutboxEvent save(ProviderOutboxEvent outboxEvent) {
        return jpaOutboxEventRepository.save(outboxEvent);
    }
}