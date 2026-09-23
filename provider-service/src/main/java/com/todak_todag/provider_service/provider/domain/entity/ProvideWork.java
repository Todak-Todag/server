package com.todak_todag.provider_service.provider.domain.entity;

import com.todak_todag.provider_service.global.common.BaseAuditableEntity;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_provide_works")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProvideWork extends BaseAuditableEntity {

    // 월(1) ~ 일(7). date.getDayOfWeek().getValue() 와 같은 기준이다
    private static final int MIN_DAY = 1;
    private static final int MAX_DAY = 7;

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
        validate(day, startedAt, finishedAt);

        return new ProvideWork(serviceOfferingId, day, startedAt, finishedAt);
    }

    public void update(Integer day, LocalTime startedAt, LocalTime finishedAt) {
        validate(day, startedAt, finishedAt);

        this.day = day;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    // 요청 DTO의 @Min/@Max는 400을 빠르게 돌려주는 1차 방어이고, 여기가 최종 방어선이다
    // HTTP가 아닌 경로로 들어와도 매칭되지 않는 요일의 일정은 만들 수 없다
    private static void validate(Integer day, LocalTime startedAt, LocalTime finishedAt) {
        if (day == null || day < MIN_DAY || day > MAX_DAY) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_INVALID_DAY);
        }

        if (!finishedAt.isAfter(startedAt)) {
            throw new BusinessException(ProviderErrorCode.PROVIDE_WORK_INVALID_TIME_RANGE);
        }
    }

    public boolean overlaps(Integer day, LocalTime startedAt, LocalTime finishedAt) {
        return this.day.equals(day)
                && this.startedAt.isBefore(finishedAt)
                && startedAt.isBefore(this.finishedAt);
    }
}