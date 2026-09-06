package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
