package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 주어진 상태이면서 수행 결과가 아직 등록되지 않은 일정 수 — 소프트 삭제분은 양쪽 모두 제외
    // "결과 없음"은 파생 쿼리로 표현할 수 없어(연관관계 없는 별도 테이블) JPQL로 작성
    @Query("""
            select count(schedule)
            from ServiceSchedule schedule
            where schedule.carePlanId = :carePlanId
              and schedule.deletedAt is null
              and schedule.status in :statuses
              and not exists (
                  select 1
                  from CarePlanServiceResult result
                  where result.serviceScheduleId = schedule.id
                    and result.deletedAt is null
              )
            """)
    long countMissingResult(
            @Param("carePlanId") UUID carePlanId,
            @Param("statuses") Collection<ScheduleStatus> statuses
    );
}
