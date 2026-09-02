package com.todak_todag.schedule_service.schedule.application.port;

import java.time.LocalDate;
import java.util.UUID;

// schedule-service -> care-plan-service Internal API 호출 추상화
public interface CarePlanPort {

    // servicePreferenceId 기준으로 소속 Care Plan의 carePlanId/finishDate/patientId를 조회
    CarePlanRange findCarePlanRange(UUID servicePreferenceId);

    record CarePlanRange(
            UUID carePlanId,
            LocalDate finishDate,
            UUID patientId
    ) {
    }
}
