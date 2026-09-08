package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPublishPort;
import com.todak_todag.provider_service.provider.application.service.command.OutboxEventCommandService;
import com.todak_todag.provider_service.provider.application.service.query.OutboxEventQueryService;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

// 아웃박스에 쌓인 미발행 이벤트를 실제로 브로커에 보낸다
// 트리거(@Scheduled)는 infrastructure/messaging/OutboxRelayScheduler에 있다
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayFacade {

    // 한 번의 폴링에서 처리할 최대 건수. 무제한 조회로 인한 부하를 막는다
    private static final int BATCH_SIZE = 100;

    private final OutboxEventQueryService outboxEventQueryService;
    private final OutboxEventCommandService outboxEventCommandService;
    private final MatchingEventPublishPort matchingEventPublishPort;
    private final ObjectMapper objectMapper;

    public void relay() {
        List<ProviderOutboxEvent> pendingEvents = outboxEventQueryService.findPending(BATCH_SIZE);

        for (ProviderOutboxEvent pendingEvent : pendingEvents) {
            relayOne(pendingEvent);
        }
    }

    // 1건이 실패해도 배치 전체를 멈추지 않는다
    // 실패한 건은 발행 시각이 비어 있어 다음 폴링에서 다시 잡힌다
    private void relayOne(ProviderOutboxEvent pendingEvent) {
        try {
            switch (pendingEvent.getEventType()) {
                case PROVIDER_MATCHED -> matchingEventPublishPort.publishMatched(
                        objectMapper.readValue(pendingEvent.getPayload(), ProviderMatchedEvent.class)
                );
                case PROVIDER_MATCH_FAILED -> matchingEventPublishPort.publishMatchFailed(
                        objectMapper.readValue(pendingEvent.getPayload(), ProviderMatchFailedEvent.class)
                );
            }

            outboxEventCommandService.markPublished(pendingEvent.getId());
        } catch (Exception e) {
            log.error("[Provider] 아웃박스 이벤트 발행 실패 outboxEventId={} eventType={}",
                    pendingEvent.getId(), pendingEvent.getEventType(), e);

            outboxEventCommandService.recordFailure(pendingEvent.getId(), e.getMessage());
        }
    }
}