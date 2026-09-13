package com.spring.careplanservice.careplan.domain.entity;

import com.spring.careplanservice.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@Table(
        name = "p_care_plan_outbox_events",
        schema = "care_plan_schema"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarePlanOutboxEvent extends BaseAuditEntity {
    public static final int MAX_RETRY_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outbox_event_id")
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, updatable = false)
    private CarePlanOutboxEventType eventType;

    @Column(
            name = "payload",
            nullable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private CarePlanOutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "published_at")
    private Instant publishedAt;

    // 다중 인스턴스 환경에서 PENDING -> PROCESSING 선점 시
    // 낙관적 락으로 동시 선점을 방지하기 위한 버전 컬럼
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    private CarePlanOutboxEvent(
            UUID aggregateId,
            CarePlanOutboxEventType eventType,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = CarePlanOutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static CarePlanOutboxEvent create(
            UUID aggregateId,
            CarePlanOutboxEventType eventType,
            String payload
    ) {
        return new CarePlanOutboxEvent(
                aggregateId,
                eventType,
                payload
        );
    }

    // RabbitMQ 발행 성공 시 호출
    public void markSent() {
        this.status = CarePlanOutboxEventStatus.SENT;
        this.publishedAt = Instant.now();
    }

    // RabbitMQ 발행 실패 시 호출 (claim()으로 선점된 PROCESSING 상태에서 호출됨)
    // 실패 횟수가 3회에 도달하면 FAILED로 전환하고,
    // 그 전까지는 PENDING으로 되돌려 다음 폴링(findPending)에서 다시 선점·재시도할 수 있게 한다.
    public void recordFailure(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;

        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = CarePlanOutboxEventStatus.FAILED;
        } else {
            this.status = CarePlanOutboxEventStatus.PENDING;
        }
    }

    public boolean isPending() {
        return this.status == CarePlanOutboxEventStatus.PENDING;
    }

    public boolean isFailed() {
        return this.status == CarePlanOutboxEventStatus.FAILED;
    }

    // 다중 인스턴스 환경에서 Relay가 발행을 시도하기 전에 선점한다.
    // 이 메서드 호출 후 저장(save) 시점에 @Version 값이 이미 바뀌어 있으면
    // 다른 인스턴스가 먼저 선점한 것이므로 낙관적 락 예외가 발생한다.
    public void startProcessing() {
        this.status = CarePlanOutboxEventStatus.PROCESSING;
    }

    // 지금 이 순간에도 여전히 PROCESSING이고, 마지막 갱신 시각이 threshold보다 오래되었는지
    // (= 그 사이 다른 인스턴스가 이미 복구 후 재선점하지 않았는지)를 재검증한다.
    // findStuckProcessing() 조회 시점과 실제 복구 시점 사이의 race condition을 막기 위한 재확인이다.
    public boolean isStuckProcessing(Instant threshold) {
        return this.status == CarePlanOutboxEventStatus.PROCESSING
                && getUpdatedAt() != null
                && getUpdatedAt().isBefore(threshold);
    }

    // 선점(PROCESSING) 이후 발행/실패 처리 없이 인스턴스가 죽어
    // 오래도록 멈춰 있는 이벤트를 다음 폴링에서 다시 시도할 수 있도록 되돌린다.
    // 호출 전에 isStuckProcessing(threshold)로 검증해야 한다.
    public void revertStuckProcessing() {
        this.status = CarePlanOutboxEventStatus.PENDING;
    }

    // 운영자가 FAILED 이벤트를 재처리 대상으로 되돌린다.
    // 호출 전에 FAILED 상태인지(isFailed())는 호출부에서 검증해야 한다.
    public void retryFromFailed() {
        this.status = CarePlanOutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.lastErrorMessage = null;
    }
}
