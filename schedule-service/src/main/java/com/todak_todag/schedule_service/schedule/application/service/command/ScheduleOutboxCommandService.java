package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 아웃박스 레코드에 대한 상태 변경 전용 서비스
// 트랜잭션이 이미 열려 있으면 그 트랜잭션에 그대로 참여
@Service
@RequiredArgsConstructor
public class ScheduleOutboxCommandService {

    private final ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;

    // 도메인 이벤트를 실제로 발행하는 대신, 같은 트랜잭션 안에서 PENDING 상태로 적재만 진행
    @Transactional
    public void enqueue(String eventType, UUID aggregateId, String payload) {
        scheduleOutboxEventCommandRepository.save(ScheduleOutboxEvent.create(eventType, aggregateId, payload));
    }

    // 릴레이가 브로커 발행에 성공한 뒤 호출 — 레코드를 SENT로 확정해 다음 폴링에서 제외
    @Transactional
    public void markSent(UUID outboxEventId) {
        ScheduleOutboxEvent event = scheduleOutboxEventCommandRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 아웃박스 이벤트입니다. outboxEventId=" + outboxEventId));

        event.markSent();
        scheduleOutboxEventCommandRepository.save(event);
    }

    // 릴레이가 브로커 발행에 실패한 뒤 호출 — 실패 이력을 남기고, 엔티티가 스스로 재시도 상한
    // 초과 여부를 판단해 FAILED로 전환할지(DLQ) PENDING을 유지해 다음 주기에 재시도할지 결정
    @Transactional
    public void recordFailure(UUID outboxEventId, String errorMessage) {
        ScheduleOutboxEvent event = scheduleOutboxEventCommandRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 아웃박스 이벤트입니다. outboxEventId=" + outboxEventId));

        event.recordFailure(errorMessage);
        scheduleOutboxEventCommandRepository.save(event);
    }
}
