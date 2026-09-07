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

        // Care Plan 기간은 30일 고정
        private static final long CARE_PLAN_PERIOD_DAYS = 30L;

        // Internal API 응답에 startDate가 없어 고정 기간으로 역산 (finishDate = startDate + 29일)
        public LocalDate startDate() {
            return finishDate.minusDays(CARE_PLAN_PERIOD_DAYS - 1);
        }

        // 요청 날짜가 Care Plan 일정 범위(start_date~finish_date) 안인지
        public boolean covers(LocalDate date) {
            return !date.isBefore(startDate()) && !date.isAfter(finishDate);
        }
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
