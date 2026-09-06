package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanServiceResultCommandRepository {

    CarePlanServiceResult save(CarePlanServiceResult carePlanServiceResult);

    boolean existsByServiceScheduleId(UUID serviceScheduleId);

    // 특정 서비스 일정에 등록된 수행 결과 1건 — CarePlanCompleted 페이로드의 serviceResultId 결정용
    Optional<CarePlanServiceResult> findByServiceScheduleId(UUID serviceScheduleId);
}
