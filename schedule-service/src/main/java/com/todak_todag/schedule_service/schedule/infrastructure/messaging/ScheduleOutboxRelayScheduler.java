package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.schedule.application.facade.ScheduleOutboxRelayFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 아웃박스 릴레이의 트리거
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "schedule.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleOutboxRelayScheduler {

    private final ScheduleOutboxRelayFacade scheduleOutboxRelayFacade;

    @Scheduled(fixedDelayString = "${schedule.outbox.relay.fixed-delay-ms:5000}")
    public void relay() {
        scheduleOutboxRelayFacade.relay();
    }
}
