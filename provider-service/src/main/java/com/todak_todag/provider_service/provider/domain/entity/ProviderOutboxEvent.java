package com.todak_todag.provider_service.provider.domain.entity;

import com.todak_todag.provider_service.global.common.BaseUpdatableEntity;
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

// 트랜잭션 아웃박스 — 매칭 결과를 브로커로 보내기 전에 DB에 먼저 적재한다
// 발행이 실패해도 레코드가 남아 릴레이가 다음 주기에 다시 시도한다
@Getter
@Entity
@Table(name = "p_provider_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderOutboxEvent extends BaseUpdatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_event_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private OutboxEventType eventType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    // 발행에 성공한 시각. null이면 아직 발행되지 않은 건이다
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    // 폴링이 1초 주기라 약 3분. 브로커 재시작·순단은 이 안에 복구되어 자동으로 발행된다
    // 이보다 긴 장애는 사람이 개입할 장애로 보고, 한도를 넘긴 건은 삭제하지 않고 남긴다
    public static final int MAX_RETRY_COUNT = 180;

    public static ProviderOutboxEvent of(
            OutboxEventType eventType,
            UUID aggregateId,
            String payload
    ) {
        ProviderOutboxEvent outboxEvent = new ProviderOutboxEvent();
        outboxEvent.eventType = eventType;
        outboxEvent.aggregateId = aggregateId;
        outboxEvent.payload = payload;

        return outboxEvent;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    // 발행 실패는 상태를 바꾸지 않고 다음 폴링에서 다시 시도한다
    // 시도 횟수가 MAX_RETRY_COUNT에 닿으면 조회 대상에서 빠진다 (삭제하지 않고 원인 확인용으로 남긴다)
    public void recordFailure(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
    }

    public boolean isRetryExhausted() {
        return this.retryCount >= MAX_RETRY_COUNT;
    }
}