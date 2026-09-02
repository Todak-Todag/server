package com.todak_todag.schedule_service.schedule.domain.entity;

import com.todak_todag.schedule_service.global.common.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// 트랜잭션 아웃박스 — 도메인 이벤트를 실제 브로커로 보내기 전, 같은 로컬 트랜잭션 안에서 먼저 적재
@Entity
@Getter
@Table(name = "p_schedule_outbox_events", schema = "schedule_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleOutboxEvent extends BaseUpdatableEntity {

    // 발행 재시도 횟수
    public static final int MAX_RETRY_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_event_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "published_at")
    private Instant publishedAt;

    private ScheduleOutboxEvent(String eventType, UUID aggregateId, String payload) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    // 커맨드 트랜잭션 안에서 적재할 아웃박스 레코드 생성
    public static ScheduleOutboxEvent create(String eventType, UUID aggregateId, String payload) {
        if (eventType == null || eventType.isBlank() || aggregateId == null || payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("아웃박스 이벤트 생성에 필요한 값이 누락되었습니다.");
        }

        return new ScheduleOutboxEvent(eventType, aggregateId, payload);
    }

    // 릴레이가 실제 발행에 성공했을 때 호출
    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.publishedAt = Instant.now();
    }

    // 릴레이가 발행에 실패했을 때 호출
    // retryCount를 늘리고 실패 사유를 남긴 뒤, MAX_RETRY_COUNT에 도달하면 FAILED로 전환
    // 아직 상한 미만이면 PENDING을 유지해 다음 폴링 주기에 다시 시도
    public void recordFailure(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;

        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = OutboxEventStatus.FAILED;
        }
    }
}
