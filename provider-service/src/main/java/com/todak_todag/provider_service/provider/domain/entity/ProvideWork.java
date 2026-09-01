package com.todak_todag.provider_service.provider.domain.entity;

import com.todak_todag.provider_service.global.common.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_provide_works")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProvideWork extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "provide_work_id")
    private UUID id;

    @Column(name = "service_offering_id", nullable = false, updatable = false)
    private UUID serviceOfferingId;

    @Column(name = "day", nullable = false)
    private Integer day;

    @Column(name = "started_at", nullable = false)
    private LocalTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalTime finishedAt;

    private ProvideWork(UUID serviceOfferingId, Integer day, LocalTime startedAt, LocalTime finishedAt) {
        this.serviceOfferingId = serviceOfferingId;
        this.day = day;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static ProvideWork of(UUID serviceOfferingId, Integer day, LocalTime startedAt, LocalTime finishedAt) {
        return new ProvideWork(serviceOfferingId, day, startedAt, finishedAt);
    }

    public void update(Integer day, LocalTime startedAt, LocalTime finishedAt) {
        this.day = day;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public boolean overlaps(Integer day, LocalTime startedAt, LocalTime finishedAt) {
        return this.day.equals(day)
                && this.startedAt.isBefore(finishedAt)
                && startedAt.isBefore(this.finishedAt);
    }
}