package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.OutboxEventStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataScheduleOutboxEventRepository extends JpaRepository<ScheduleOutboxEvent, UUID> {

    // 릴레이 폴링용 조회 — 같은 상태(PENDING) 내에서 오래 대기한 이벤트부터 처리되도록 생성 순서로 정렬
    List<ScheduleOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
