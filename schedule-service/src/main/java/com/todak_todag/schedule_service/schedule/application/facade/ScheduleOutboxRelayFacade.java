package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ScheduleOutboxEventResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ScheduleOutboxQueryService;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// 아웃박스에 적재된 PENDING 이벤트를 실제로 발행하는 유스케이스 조합
// 트리거(@Scheduled)는 infrastructure/messaging/ScheduleOutboxRelayScheduler에 존재
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleOutboxRelayFacade {

    // 한 번의 폴링에서 처리할 최대 이벤트 수 (무제한 조회로 인한 부하를 막기 위한 최소한의 안전장치)
    private static final int BATCH_SIZE = 100;

    private final ScheduleOutboxQueryService scheduleOutboxQueryService;
    private final ScheduleOutboxCommandService scheduleOutboxCommandService;
    private final ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;
    private final ProviderReMatchEventPort providerReMatchEventPort;

    // 한 번의 폴링 주기 전체를 처리
    // ScheduleOutboxRelayScheduler(@Scheduled)가 이 메서드를 반복 호출
    public void relay() {
        List<ScheduleOutboxEventResult> pendingEvents = scheduleOutboxQueryService.findPending(BATCH_SIZE);

        for (ScheduleOutboxEventResult pendingEvent : pendingEvents) {
            relayOne(pendingEvent);
        }
    }

    // 이벤트 1건을 검증 → 역직렬화 → 발행 → 상태 갱신까지 처리
    // 어느 단계에서든 예외가 나면 그 이벤트만 recordFailure로 실패 처리하고, 배치 전체는 중단하지 않음
    private void relayOne(ScheduleOutboxEventResult pendingEvent) {
        try {
            // TODO: 현재 아웃박스에 적재되는 이벤트 타입은 ProviderReMatched, 추후 추가 예정
            // 다른 타입이 섞여 들어오면(잘못된 적재 등) 정상 처리할 방법이 없으므로 실패로 기록
            if (!ProviderReMatchEventPort.EVENT_TYPE.equals(pendingEvent.eventType())) {
                throw new IllegalStateException("처리할 수 없는 이벤트 타입입니다. eventType=" + pendingEvent.eventType());
            }

            ProviderReMatchEventPort.ProviderReMatchEvent event =
                    providerReMatchEventPayloadSerializer.deserialize(pendingEvent.payload());

            providerReMatchEventPort.publish(event);

            scheduleOutboxCommandService.markSent(pendingEvent.outboxEventId());
        } catch (Exception e) {
            log.error("[Schedule] 아웃박스 이벤트 발행 실패 outboxEventId={}", pendingEvent.outboxEventId(), e);
            scheduleOutboxCommandService.recordFailure(pendingEvent.outboxEventId(), e.getMessage());
        }
    }
}
