package com.todak_todag.provider_service.provider.infrastructure.persistence.query;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventQueryRepositoryImpl implements OutboxEventQueryRepository {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;

    @Override
    public List<ProviderOutboxEvent> findPending(int limit) {
        return jpaOutboxEventRepository
                .findAllByPublishedAtIsNullAndRetryCountLessThanOrderByCreatedAtAsc(
                        ProviderOutboxEvent.MAX_RETRY_COUNT, PageRequest.of(0, limit));
    }

    @Override
    public Optional<ProviderOutboxEvent> findById(UUID outboxEventId) {
        return jpaOutboxEventRepository.findById(outboxEventId);
    }
}