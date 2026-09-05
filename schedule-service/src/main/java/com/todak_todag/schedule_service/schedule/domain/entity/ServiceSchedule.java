package com.todak_todag.schedule_service.schedule.domain.entity;

import com.todak_todag.schedule_service.global.common.BaseAuditableEntity;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Table 명세서상 created_at/by(Not Null) + updated_at/by(Not Null) + deleted_at/by(Nullable)를 모두 가지므로
// 4종 Base 중 감사 필드 전체를 포함하는 BaseAuditableEntity를 상속한다
@Entity
@Getter
@Table(name = "p_service_schedules", schema = "schedule_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceSchedule extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_schedule_id", nullable = false, updatable = false)
    private UUID id;

    // Table 명세서(schedule-service.md 2장)의 p_service_schedules.care_plan_id — 논리 FK(→ p_care_plans.care_plan_id), Not Null
    // DB가 서비스별로 분리되어 있어 FK 제약 없이 UUID 값만 보관한다
    // ⚠️ 03/04번(서비스 일정 변경/취소)은 servicePreferenceId 기준으로 care-plan-service Internal API를 호출해
    //    carePlanId/finishDate/patientId를 조회하도록 이미 구현되어 있다(5.5절). 이 필드가 생겨도
    //    finishDate(일정 범위 검증)와 patientId(소유권 검증)는 여전히 원격 조회가 필요하므로 그 호출은 그대로 유지한다
    //    — 즉 이 필드는 03/04번 로직을 대체하지 않고, Table 명세서와의 스키마 정합성을 맞추기 위한 것이다
    @Column(name = "care_plan_id", nullable = false)
    private UUID carePlanId;

    @Column(name = "service_preference_id", nullable = false)
    private UUID servicePreferenceId;

    @Column(name = "service_offering_id", nullable = false)
    private UUID serviceOfferingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleStatus status;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private ServiceSchedule(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        this.carePlanId = carePlanId;
        this.servicePreferenceId = servicePreferenceId;
        this.serviceOfferingId = serviceOfferingId;
        this.date = date;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.status = ScheduleStatus.SCHEDULED;
    }

    // ProviderMatched 이벤트를 수신해 제공자 매칭이 확정
    // 일정이 확정되었기 때문에 상태는 SCHEDULED로 고정
    public static ServiceSchedule confirm(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {

        // 일정 생성에 필요한 필수 식별자 및 일정 시간 정보를 검증
        validateConfirmParameters(
                carePlanId,
                servicePreferenceId,
                serviceOfferingId,
                date,
                startedAt,
                finishedAt
        );

        return new ServiceSchedule(
                carePlanId,
                servicePreferenceId,
                serviceOfferingId,
                date,
                startedAt,
                finishedAt
        );
    }

    // 예정된 일정을 변경
    // SCHEDULED 상태에서만 일정 변경이 가능
    public void rescheduling() {
        if (status != ScheduleStatus.SCHEDULED) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING);
        }

        this.status = ScheduleStatus.RESCHEDULING;
    }

    // 예정된 일정을 취소
    // SCHEDULED, RESCHEDULING 상태에서만 취소 가능
    // 이떄 취소 사유에 대한 내용은 필수 작성
    public void cancel(String reason) {
        if (status != ScheduleStatus.SCHEDULED && status != ScheduleStatus.RESCHEDULING) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL);
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        this.status = ScheduleStatus.CANCELED;
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();
    }

    // 예정대로 진행 완료
    // SCHEDULED만 COMPLETED 처리를 허용 — 이미 취소/연기중/변경완료/완료/부도 상태는 409
    public void complete() {
        if (status != ScheduleStatus.SCHEDULED) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED);
        }

        this.status = ScheduleStatus.COMPLETED;
    }

    // 예정대로 진행했어야 했지만 아무 조치 없이 종료 시각이 지남
    // SCHEDULED만 NO_SHOW 처리 허용 — 이미 취소/연기중/변경완료/완료/부도 상태는 409
    public void markNoShow() {
        if (status != ScheduleStatus.SCHEDULED) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED);
        }

        this.status = ScheduleStatus.NO_SHOW;
    }

    // 서비스 수행 결과 등록 가능 여부 검증
    public void assertResultRegistrable() {
        if (status != ScheduleStatus.COMPLETED && status != ScheduleStatus.NO_SHOW) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_INVALID_SCHEDULE_STATUS);
        }
    }

    // 일정 생성에 필요한 필수 식별자 및 일정 시간 정보를 검증
    private static void validateConfirmParameters(
            UUID carePlanId,
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {

        // 케어플랜 ID, 서비스 희망 일정 ID, 서비스 제공 ID는 모두 Not Null 컬럼이므로 필수
        if (carePlanId == null || servicePreferenceId == null || serviceOfferingId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 일정 날짜는 오늘보다 이후인 날짜만 허용
        if (date == null || !date.isAfter(LocalDate.now())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 일정 시작 시간과 종료 시간은 필수
        if (startedAt == null || finishedAt == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 시작 시간과 종료 시간은 일정 날짜와 동일한 날짜여야 함
        if (!date.equals(startedAt.toLocalDate())
                || !date.equals(finishedAt.toLocalDate())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 종료 시간은 시작 시간보다 같거나 빠를 수 없음
        if (!finishedAt.isAfter(startedAt)) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }
}
