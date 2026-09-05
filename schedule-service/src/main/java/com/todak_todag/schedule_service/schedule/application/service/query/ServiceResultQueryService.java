package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceResultQueryService {

    private final CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    // 서비스 수행 결과 목록 조회
    @Transactional(readOnly = true)
    public Page<ServiceResultSearchResult> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            Pageable pageable
    ) {
        return carePlanServiceResultQueryRepository.search(servicePreferenceIds, serviceOfferingIds, pageable)
                .map(ServiceResultSearchResult::from);
    }
}
