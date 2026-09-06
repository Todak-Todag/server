package com.todak_todag.schedule_service.schedule.domain.repository.query;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.util.Optional;
import java.util.UUID;

public interface ServiceMatchingAttemptQueryRepository {

    // 단건 조회 — 소프트 삭제된 기록은 제외
    Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId);
}
