package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
