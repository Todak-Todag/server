package com.todak_todag.social_worker_service.matching.domain.entity;

import com.todak_todag.social_worker_service.global.common.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "p_social_worker_matching_results",
        schema = "social_worker_schema"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialWorkerMatchingResult extends BaseAuditableEntity {

    @Id
    @Column(name = "matching_result_id", nullable = false)
    private UUID matchingResultId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "social_worker_id")
    private UUID socialWorkerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private MatchingStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    private SocialWorkerMatchingResult(
            UUID matchingResultId,
            UUID patientId,
            MatchingStatus status,
            Instant requestedAt
    ) {
        this.matchingResultId = matchingResultId;
        this.patientId = patientId;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public static SocialWorkerMatchingResult requested(
            UUID patientId
    ) {
        return new SocialWorkerMatchingResult(
                UUID.randomUUID(),
                patientId,
                MatchingStatus.REQUESTED,
                Instant.now()
        );
    }

    public void assign(
            UUID socialWorkerId
    ) {
        if (this.status != MatchingStatus.REQUESTED) {
            throw new IllegalStateException(
                    "REQUESTED 상태의 매칭만 배정할 수 있습니다."
            );
        }

        this.socialWorkerId = socialWorkerId;
        this.status = MatchingStatus.ACTIVE;
        this.assignedAt = Instant.now();
    }

    public void fail() {
        if (this.status != MatchingStatus.REQUESTED) {
            return;
        }

        this.status = MatchingStatus.FAILED;
    }

    public void end() {
        if (this.status != MatchingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "ACTIVE 상태의 매칭만 종료할 수 있습니다."
            );
        }

        this.status = MatchingStatus.ENDED;
    }
}