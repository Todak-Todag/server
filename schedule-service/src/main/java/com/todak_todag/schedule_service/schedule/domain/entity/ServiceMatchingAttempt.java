package com.todak_todag.schedule_service.schedule.domain.entity;

import com.todak_todag.schedule_service.global.common.BaseAuditableEntity;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_service_matching_attempts", schema = "schedule_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceMatchingAttempt extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "matching_attempt_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "care_plan_id", nullable = false)
    private UUID carePlanId;

    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Column(name = "provide_service_id", nullable = false)
    private UUID provideServiceId;

    @Column(name = "service_preference_id", nullable = false)
    private UUID servicePreferenceId;

    @Column(name = "service_offering_id")
    private UUID serviceOfferingId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_slot")
    private PreferredTimeSlot preferredTimeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchingAttemptStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "matched_at")
    private Instant matchedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    private ServiceMatchingAttempt(
            UUID carePlanId,
            UUID regionId,
            UUID provideServiceId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            PreferredTimeSlot preferredTimeSlot,
            MatchingAttemptStatus status,
            String failureReason,
            Instant matchedAt,
            Instant failedAt
    ) {
        this.carePlanId = carePlanId;
        this.regionId = regionId;
        this.provideServiceId = provideServiceId;
        this.servicePreferenceId = servicePreferenceId;
        this.serviceOfferingId = serviceOfferingId;
        this.date = date;
        this.preferredTimeSlot = preferredTimeSlot;
        this.status = status;
        this.failureReason = failureReason;
        this.matchedAt = matchedAt;
        this.failedAt = failedAt;
    }

    // 매칭 시도 결과 기록
    public static ServiceMatchingAttempt record(
            UUID carePlanId,
            UUID regionId,
            UUID provideServiceId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            PreferredTimeSlot preferredTimeSlot,
            MatchingAttemptStatus status,
            String failureReason,
            Instant matchedAt,
            Instant failedAt
    ) {
        return new ServiceMatchingAttempt(
                carePlanId,
                regionId,
                provideServiceId,
                servicePreferenceId,
                serviceOfferingId,
                date,
                preferredTimeSlot,
                status,
                failureReason,
                matchedAt,
                failedAt
        );
    }
}
