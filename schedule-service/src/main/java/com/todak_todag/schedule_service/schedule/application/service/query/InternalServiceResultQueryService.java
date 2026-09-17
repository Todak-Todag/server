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
    // 존재 검증과 carePlanId 확보를 조인 쿼리 한 번으로 처리 — 결과/일정 중 하나라도 유효하지 않으면 검증 실패로 보고 404
    @Transactional(readOnly = true)
    public InternalServiceResultDetailResult findById(UUID serviceResultId) {

        UUID carePlanId = carePlanServiceResultQueryRepository.findCarePlanIdByServiceResultId(serviceResultId)
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_NOT_FOUND));

        return InternalServiceResultDetailResult.of(carePlanId, serviceResultId);
    }
}
