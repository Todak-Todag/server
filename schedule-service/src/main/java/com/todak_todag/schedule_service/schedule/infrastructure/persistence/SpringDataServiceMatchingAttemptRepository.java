package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataServiceMatchingAttemptRepository extends JpaRepository<ServiceMatchingAttempt, UUID> {

    // 소프트 삭제된 기록은 조회 대상에서 제외
    Optional<ServiceMatchingAttempt> findByIdAndDeletedAtIsNull(UUID id);

    // 위와 같은 단건 조회지만 로우에 쓰기 락을 걸어 같은 매칭 시도에 대한 동시 재시도 접수를 직렬화
    // 락 획득 순간의 최신 상태를 읽어야 하므로 1차 캐시가 아닌 DB를 다시 보게 되는 경로여야 함
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt
            from ServiceMatchingAttempt attempt
            where attempt.id = :id
              and attempt.deletedAt is null
            """)
    Optional<ServiceMatchingAttempt> findByIdForUpdate(@Param("id") UUID id);

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

    // 위 countUnresolvedByStatus와 같은 대상을 세지 않고 실체로 가져옴 — 보정 스윕이 EXPIRED로 종결 처리할 대상
    // where 절이 어긋나면 "판정에는 걸리는데 종결은 안 되는" 건이 생겨 스윕이 매일 같은 케어플랜을 다시 잡으므로 두 쿼리의 조건은 반드시 동일하게 유지
    @Query("""
            select attempt
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
    List<ServiceMatchingAttempt> findUnresolvedByStatus(
            @Param("carePlanId") UUID carePlanId,
            @Param("status") MatchingAttemptStatus status
    );

    // 보정 스윕 대상 케어플랜 ID 조회 — "활동이 끝난 지 유예기간이 지났는데 미해소 매칭 실패가 남아있는" 케어플랜
    //
    // 조건을 하나씩 보면
    //   (1) status = FAILED + 일정 레코드 없음 → 재매칭이 끝내 시도되지 않은 초기 매칭 실패가 남아있음
    //   (2) 케어플랜에 일정이 1건 이상 있다 → CarePlanCompleted 페이로드의 status/serviceResultId를 채울 "마지막 일정"이 존재해야 함
    //                                  전 서비스가 초기 매칭에 실패해 일정이 0건인 케어플랜은 실을 값이 없으므로 여기서 제외
    //   (3) 일정/매칭시도의 마지막 date가 모두 임계일 이하 → 이 케어플랜에서 더 일어날 일이 없음
    //
    // GREATEST(max(일정.date), max(시도.date)) <= 임계일 을 두 개의 비교로 나눠 쓴 것이라 의미는 같음
    // (2) 덕분에 일정 쪽 max는 항상 non-null이고, 매칭시도 쪽은 이 쿼리의 출발점이라 역시 non-null
    @Query("""
            select distinct attempt.carePlanId
            from ServiceMatchingAttempt attempt
            where attempt.deletedAt is null
              and attempt.status = :status
              and not exists (
                  select 1
                  from ServiceSchedule schedule
                  where schedule.servicePreferenceId = attempt.servicePreferenceId
                    and schedule.deletedAt is null
              )
              and exists (
                  select 1
                  from ServiceSchedule anySchedule
                  where anySchedule.carePlanId = attempt.carePlanId
                    and anySchedule.deletedAt is null
              )
              and (
                  select max(scheduleDate.date)
                  from ServiceSchedule scheduleDate
                  where scheduleDate.carePlanId = attempt.carePlanId
                    and scheduleDate.deletedAt is null
              ) <= :lastActivityThreshold
              and (
                  select max(attemptDate.date)
                  from ServiceMatchingAttempt attemptDate
                  where attemptDate.carePlanId = attempt.carePlanId
                    and attemptDate.deletedAt is null
              ) <= :lastActivityThreshold
            order by attempt.carePlanId
            """)
    List<UUID> findSweepTargetCarePlanIds(
            @Param("status") MatchingAttemptStatus status,
            @Param("lastActivityThreshold") LocalDate lastActivityThreshold,
            Pageable pageable
    );
}
