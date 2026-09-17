package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarePlanServiceResultQueryRepository {

    // 단건 조회 — 소프트 삭제된 결과는 제외
    Optional<CarePlanServiceResult> findById(UUID serviceResultId);

    // [내부 API] 수행 결과가 속한 케어플랜 ID 조회 — care_plan_id는 p_service_schedules에만 있어 조인이 필요
    // 결과와 일정 중 하나라도 없거나 소프트 삭제되면 빈 Optional을 반환
    Optional<UUID> findCarePlanIdByServiceResultId(UUID serviceResultId);

    // 서비스 수행 결과 목록 조회
    Page<CarePlanServiceResult> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            Pageable pageable
    );
}
