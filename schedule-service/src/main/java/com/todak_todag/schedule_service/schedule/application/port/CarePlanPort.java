package com.todak_todag.schedule_service.schedule.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// schedule-service -> care-plan-service Internal API 호출 추상화
public interface CarePlanPort {

    // servicePreferenceId 기준으로 소속 Care Plan의 carePlanId/finishDate/patientId를 조회
    CarePlanRange findCarePlanRange(UUID servicePreferenceId);

    // patientId(요청자 userId)가 담당하는 모든 servicePreferenceId 목록을 조회
    List<UUID> findServicePreferenceIds(UUID patientId);

    // patientId(요청자 userId)의 Care Plan 상태를 조회
    CarePlanSummary findCarePlanByPatient(UUID patientId);

    record CarePlanRange(
            UUID carePlanId,
            LocalDate finishDate,
            UUID patientId
    ) {
    }

    record CarePlanSummary(
            UUID carePlanId,
            CarePlanStatus status
    ) {

        public boolean isConfirmed() {
            return status == CarePlanStatus.CONFIRMED;
        }
    }

    // care-plan-service의 care_plan_status ENUM
    enum CarePlanStatus {
        UNDER_REVIEW,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED
    }
}
