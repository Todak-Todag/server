package com.todak_todag.schedule_service.schedule.domain.entity;

import com.todak_todag.schedule_service.global.common.BaseAuditableEntity;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_care_plan_service_results", schema = "schedule_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarePlanServiceResult extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_result_id", nullable = false, updatable = false)
    private UUID serviceResultId;

    @Column(name = "service_schedule_id", nullable = false)
    private UUID serviceScheduleId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    private CarePlanServiceResult(
            UUID serviceScheduleId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String note
    ) {
        this.serviceScheduleId = serviceScheduleId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.note = note;
    }

    // 결과는 실제 수행이 끝난 뒤에만 생성
    public static CarePlanServiceResult record(
            UUID serviceScheduleId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String note
    ) {

        // 수행 결과 생성에 필요한 필수 식별자 및 일정 시간 정보를 검증
        validateRecordParameter(
                serviceScheduleId,
                startedAt,
                finishedAt
        );

        return new CarePlanServiceResult(serviceScheduleId, startedAt, finishedAt, note);
    }

    // 수행 결과 생성에 필요한 필수 식별자 및 일정 시간 정보를 검증
    private static void validateRecordParameter(
            UUID serviceScheduleId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        // 서비스 일정 ID, 시작 일시, 종료 일시는 필수
        if (serviceScheduleId == null || startedAt == null || finishedAt == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        // 종료 일시는 시작 일시보다 빠를 수 없음
        if (finishedAt.isBefore(startedAt)) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }
}
