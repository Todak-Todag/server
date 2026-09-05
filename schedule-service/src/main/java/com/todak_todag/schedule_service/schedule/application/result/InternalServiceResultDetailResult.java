package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.time.LocalDateTime;
import java.util.UUID;

// [내부 API] 서비스 수행 결과 조회 결과
public record InternalServiceResultDetailResult(
        UUID serviceResultId,
        UUID serviceScheduleId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static InternalServiceResultDetailResult from(CarePlanServiceResult carePlanServiceResult) {
        return new InternalServiceResultDetailResult(
                carePlanServiceResult.getServiceResultId(),
                carePlanServiceResult.getServiceScheduleId(),
                carePlanServiceResult.getStartedAt(),
                carePlanServiceResult.getFinishedAt()
        );
    }
}
