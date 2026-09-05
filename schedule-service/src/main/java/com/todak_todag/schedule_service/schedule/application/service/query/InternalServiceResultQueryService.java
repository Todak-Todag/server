package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.result.InternalServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalServiceResultQueryService {

    private final CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    // [내부 API] 서비스 수행 결과 조회
    @Transactional(readOnly = true)
    public InternalServiceResultDetailResult findById(UUID serviceResultId) {

        return carePlanServiceResultQueryRepository.findById(serviceResultId)
                .map(InternalServiceResultDetailResult::from)
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND));
    }
}
