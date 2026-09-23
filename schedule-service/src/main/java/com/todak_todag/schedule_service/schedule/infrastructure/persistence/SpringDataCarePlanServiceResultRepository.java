package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataCarePlanServiceResultRepository extends JpaRepository<CarePlanServiceResult, UUID> {

    boolean existsByServiceScheduleIdAndDeletedAtIsNull(UUID serviceScheduleId);

    Optional<CarePlanServiceResult> findByServiceResultIdAndDeletedAtIsNull(UUID serviceResultId);

    // 일정당 결과는 최대 1건이므로 단건으로 조회
    Optional<CarePlanServiceResult> findByServiceScheduleIdAndDeletedAtIsNull(UUID serviceScheduleId);
}
