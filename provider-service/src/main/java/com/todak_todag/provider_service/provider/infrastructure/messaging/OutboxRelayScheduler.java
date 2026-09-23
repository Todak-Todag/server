package com.todak_todag.provider_service.provider.infrastructure.messaging;

import com.todak_todag.provider_service.provider.application.facade.OutboxRelayFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 아웃박스 릴레이의 트리거
// ponytail: 단일 인스턴스 전제. 여러 인스턴스를 띄우면 같은 레코드를 중복 발행할 수 있어
//           그때는 조회에 FOR UPDATE SKIP LOCKED가 필요하다
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "provider.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelayFacade outboxRelayFacade;

    @Scheduled(fixedDelayString = "${provider.outbox.relay.fixed-delay-ms:1000}")
    public void relay() {
        outboxRelayFacade.relay();
    }
}