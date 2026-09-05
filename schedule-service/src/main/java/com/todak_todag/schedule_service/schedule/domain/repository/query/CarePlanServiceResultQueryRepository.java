package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.util.Optional;
import java.util.UUID;

public interface CarePlanServiceResultQueryRepository {

    // 단건 조회 — 소프트 삭제된 결과는 제외
    Optional<CarePlanServiceResult> findById(UUID serviceResultId);
}
