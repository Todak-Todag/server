package com.todak_todag.schedule_service.schedule.domain.entity;

// 아웃박스 레코드의 생명주기 상태
// PENDING: 발행 대기 중(신규 적재, 또는 발행 실패 후 재시도 대기) — 릴레이가 폴링하는 유일한 상태
// SENT: 실제 브로커 발행에 성공한 최종 상태 — 더 이상 폴링 대상이 아님
// FAILED: 재시도 상한(ScheduleOutboxEvent.MAX_RETRY_COUNT)을 초과해 더 이상 자동 재시도하지 않는 최종 상태(DLQ 역할)
public enum OutboxEventStatus {

    PENDING,
    SENT,
    FAILED
}
