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

    private CarePlanOutboxEvent(
            UUID aggregateId,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = CarePlanOutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static CarePlanOutboxEvent create(
            UUID aggregateId,
            String payload
    ) {
        return new CarePlanOutboxEvent(
                aggregateId,
                payload
        );
    }

    // RabbitMQ 발행 성공 시 호출
    public void markSent() {
        this.status = CarePlanOutboxEventStatus.SENT;
        this.publishedAt = Instant.now();
    }

    // RabbitMQ 발행 실패 시 호출
    // 실패 횟수가 3회에 도달하면 FAILED로 전환하고,
    // 그 전까지는 PENDING을 유지하여 다음 폴링에서 재시도
    public void recordFailure(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;

        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = CarePlanOutboxEventStatus.FAILED;
        }
    }
}
