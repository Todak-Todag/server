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

    public SocialWorkerMatchingResult(
            UUID matchingResultId,
            UUID patientId,
            UUID socialWorkerId,
            MatchingStatus status,
            Instant requestedAt,
            Instant assignedAt
    ) {
        this.matchingResultId = matchingResultId;
        this.patientId = patientId;
        this.socialWorkerId = socialWorkerId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.assignedAt = assignedAt;
    }

    public void setSocialWorkerId(
            UUID socialWorkerId
    ) {
        this.socialWorkerId = socialWorkerId;
    }

    public void setStatus(
            MatchingStatus status
    ) {
        this.status = status;
    }

    public void setAssignedAt(
            Instant assignedAt
    ) {
        this.assignedAt = assignedAt;
    }
}