package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import com.todak_todag.provider_service.provider.application.service.command.OutboxEventCommandService;
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// 브로커로 바로 보내지 않고 아웃박스에 적재한다
// 실제 발행은 OutboxRelayFacade가 맡는다
@Component
@RequiredArgsConstructor
public class MatchingEventAdapter implements MatchingEventPort {

    private final OutboxEventCommandService outboxEventCommandService;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMatched(ProviderMatchedEvent event) {
        outboxEventCommandService.append(
                OutboxEventType.PROVIDER_MATCHED,
                event.servicePreferenceId(),
                objectMapper.writeValueAsString(event)
        );
    }

    @Override
    public void publishMatchFailed(ProviderMatchFailedEvent event) {
        outboxEventCommandService.append(
                OutboxEventType.PROVIDER_MATCH_FAILED,
                event.servicePreferenceId(),
                objectMapper.writeValueAsString(event)
        );
    }
}