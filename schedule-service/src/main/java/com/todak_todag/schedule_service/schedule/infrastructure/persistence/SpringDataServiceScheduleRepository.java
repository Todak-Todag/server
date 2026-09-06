package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataServiceScheduleRepository extends JpaRepository<ServiceSchedule, UUID> {

    // 소프트 삭제된 일정은 조회 대상에서 제외
    Optional<ServiceSchedule> findByIdAndDeletedAtIsNull(UUID id);

    // 케어플랜의 마지막 일정 1건 — 재매칭으로 대체된 이력(CHANGED)과 소프트 삭제분은 제외
    Optional<ServiceSchedule> findFirstByCarePlanIdAndStatusNotAndDeletedAtIsNullOrderByFinishedAtDescCreatedAtDesc(
            UUID carePlanId,
            ScheduleStatus status
    );

    // 케어플랜에 남아있는 진행 중 일정 수 — 소프트 삭제분은 제외
    long countByCarePlanIdAndStatusInAndDeletedAtIsNull(UUID carePlanId, Collection<ScheduleStatus> statuses);
}
