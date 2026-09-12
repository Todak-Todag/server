package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataServiceMatchingAttemptRepository extends JpaRepository<ServiceMatchingAttempt, UUID> {

    // 소프트 삭제된 기록은 조회 대상에서 제외
    Optional<ServiceMatchingAttempt> findByIdAndDeletedAtIsNull(UUID id);

    // 해당 희망 일정에서 성사된(MATCHED) 가장 최근 매칭 시도 1건 — 소프트 삭제분은 제외
    Optional<ServiceMatchingAttempt> findFirstByServicePreferenceIdAndStatusAndDeletedAtIsNullOrderByMatchedAtDescCreatedAtDesc(
            UUID servicePreferenceId,
            MatchingAttemptStatus status
    );

    // 같은 매칭 실패가 이미 기록되어 있는지 — 소프트 삭제분은 제외
    boolean existsByServicePreferenceIdAndDateAndFailedAtAndStatusAndDeletedAtIsNull(
            UUID servicePreferenceId,
            LocalDate date,
            Instant failedAt,
            MatchingAttemptStatus status
    );

    // 같은 매칭 결과가 이미 기록되어 있는지 — 소프트 삭제분은 제외
    boolean existsByServicePreferenceIdAndServiceOfferingIdAndDateAndMatchedAtAndStatusAndDeletedAtIsNull(
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            Instant matchedAt,
            MatchingAttemptStatus status
    );

    // 케어플랜에 남아있는 미해소 매칭 실패 수 — 소프트 삭제분은 양쪽 모두 제외
    // "일정 레코드 없음"은 파생 쿼리로 표현할 수 없어(연관관계 없는 별도 테이블) JPQL로 작성
    @Query("""
            select count(attempt)
            from ServiceMatchingAttempt attempt
            where attempt.carePlanId = :carePlanId
              and attempt.deletedAt is null
              and attempt.status = :status
              and not exists (
                  select 1
                  from ServiceSchedule schedule
                  where schedule.servicePreferenceId = attempt.servicePreferenceId
                    and schedule.deletedAt is null
              )
            """)
    long countUnresolvedByStatus(
            @Param("carePlanId") UUID carePlanId,
            @Param("status") MatchingAttemptStatus status
    );
}
