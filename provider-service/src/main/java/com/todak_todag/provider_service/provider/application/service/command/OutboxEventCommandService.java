package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.command.OutboxEventCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
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
                .ifPresent(outboxEvent -> {
                    outboxEvent.recordFailure(errorMessage);

                    // 한도에 닿는 순간 한 번만 남긴다. 이후로는 조회되지 않아 다시 찍히지 않는다
                    if (outboxEvent.isRetryExhausted()) {
                        log.error("[Provider] 재시도 한도 초과로 발행 대상에서 제외 outboxEventId={} eventType={} retryCount={}",
                                outboxEvent.getId(), outboxEvent.getEventType(), outboxEvent.getRetryCount());
                    }
                });
    }
}