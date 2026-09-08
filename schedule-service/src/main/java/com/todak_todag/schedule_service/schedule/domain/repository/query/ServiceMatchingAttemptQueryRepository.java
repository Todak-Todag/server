package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceMatchingAttemptQueryRepository {

    // 단건 조회 — 소프트 삭제된 기록은 제외
    Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId);

    // 매칭 시도 내역 목록 조회
    // excludeAlreadyScheduled가 true면 이미 일정이 생성된 희망 일정의 시도는 제외
    Page<ServiceMatchingAttempt> search(
            List<UUID> servicePreferenceIds,
            MatchingAttemptStatus status,
            boolean excludeAlreadyScheduled,
            Pageable pageable
    );
}
