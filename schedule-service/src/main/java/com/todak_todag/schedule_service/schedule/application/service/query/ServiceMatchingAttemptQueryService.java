package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceMatchingAttemptResult;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceMatchingAttemptQueryService {

    private final ServiceMatchingAttemptQueryRepository serviceMatchingAttemptQueryRepository;

    // 매칭 시도 단건 조회 — 존재 여부 판단은 조회하지 않고 Facade가 담당
    @Transactional(readOnly = true)
    public Optional<ServiceMatchingAttemptResult> findById(UUID matchingAttemptId) {
        return serviceMatchingAttemptQueryRepository.findById(matchingAttemptId)
                .map(ServiceMatchingAttemptResult::from);
    }

    // 매칭 시도 내역 목록 조회
    @Transactional(readOnly = true)
    public Page<MatchingAttemptSearchResult> search(
            List<UUID> servicePreferenceIds,
            MatchingAttemptStatus status,
            boolean excludeAlreadyScheduled,
            Pageable pageable
    ) {
        return serviceMatchingAttemptQueryRepository
                .search(servicePreferenceIds, status, excludeAlreadyScheduled, pageable)
                .map(MatchingAttemptSearchResult::from);
    }
}
