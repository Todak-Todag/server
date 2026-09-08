package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.command.OutboxEventCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxEventCommandService {

    private final OutboxEventCommandRepository outboxEventCommandRepository;
    private final OutboxEventQueryRepository outboxEventQueryRepository;

    public void append(OutboxEventType eventType, UUID aggregateId, String payload) {
        outboxEventCommandRepository.save(
                ProviderOutboxEvent.of(eventType, aggregateId, payload)
        );
    }

    public void markPublished(UUID outboxEventId) {
        outboxEventQueryRepository.findById(outboxEventId)
                .ifPresent(ProviderOutboxEvent::markPublished);
    }

    public void recordFailure(UUID outboxEventId, String errorMessage) {
        outboxEventQueryRepository.findById(outboxEventId)
                .ifPresent(outboxEvent -> outboxEvent.recordFailure(errorMessage));
    }
}