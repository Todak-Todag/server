package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResultSearchResult(
        UUID serviceResultId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ServiceResultSearchResult from(CarePlanServiceResult carePlanServiceResult) {
        return new ServiceResultSearchResult(
                carePlanServiceResult.getServiceResultId(),
                carePlanServiceResult.getStartedAt(),
                carePlanServiceResult.getFinishedAt()
        );
    }
}
