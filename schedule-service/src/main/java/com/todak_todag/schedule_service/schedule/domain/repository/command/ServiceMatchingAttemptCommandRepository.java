package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.util.Optional;
import java.util.UUID;

public interface ServiceMatchingAttemptCommandRepository {

    ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt);

    // 해당 서비스 희망 일정(servicePreferenceId)을 성사시킨 가장 최근 매칭 시도 1건
    Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId);
}
