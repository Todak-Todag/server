package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceScheduleQueryService {

    private final ServiceScheduleQueryRepository serviceScheduleQueryRepository;

    @Transactional(readOnly = true)
    public Optional<ServiceScheduleResult> findById(UUID serviceScheduleId) {
        return serviceScheduleQueryRepository.findById(serviceScheduleId)
                .map(ServiceScheduleResult::from);
    }

    // 서비스 일정 상세 조회
    @Transactional(readOnly = true)
    public Optional<ServiceScheduleDetailResult> findDetailById(UUID serviceScheduleId) {
        return serviceScheduleQueryRepository.findById(serviceScheduleId)
                .map(ServiceScheduleDetailResult::from);
    }

    // 서비스 일정 목록 조회
    @Transactional(readOnly = true)
    public Page<ServiceScheduleSearchResult> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            ScheduleStatus status,
            LocalDate date,
            Pageable pageable
    ) {
        return serviceScheduleQueryRepository.search(servicePreferenceIds, serviceOfferingIds, status, date, pageable)
                .map(ServiceScheduleSearchResult::from);
    }
}
