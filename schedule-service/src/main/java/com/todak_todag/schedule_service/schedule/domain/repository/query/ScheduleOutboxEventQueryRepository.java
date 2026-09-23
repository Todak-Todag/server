package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;

import java.util.List;

// 아웃박스 릴레이가 "이번 폴링 주기에 무엇을 처리할지" 찾을 때만 쓰는 조회 전용 인터페이스
public interface ScheduleOutboxEventQueryRepository {

    // 생성 순서대로 최대 limit건 조회 (배치 처리 안전장치 — Pageable 등 프레임워크 타입은 순수 인터페이스 원칙상 노출하지 않음)
    // status에 PENDING을 넘기면 릴레이가 이번에 재시도할 이벤트 목록이 됨 (SENT/FAILED는 폴링 대상에서 자연히 제외)
    List<ScheduleOutboxEvent> findByStatus(OutboxEventStatus status, int limit);
}
