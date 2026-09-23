package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboxEventQueryService {

    private final OutboxEventQueryRepository outboxEventQueryRepository;

    public List<ProviderOutboxEvent> findPending(int limit) {
        return outboxEventQueryRepository.findPending(limit);
    }
}