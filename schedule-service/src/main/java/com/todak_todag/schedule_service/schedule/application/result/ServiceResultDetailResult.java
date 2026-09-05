package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResultDetailResult(
        UUID serviceResultId,
        UUID serviceScheduleId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String note
) {

    public static ServiceResultDetailResult from(CarePlanServiceResult carePlanServiceResult) {
        return new ServiceResultDetailResult(
                carePlanServiceResult.getServiceResultId(),
                carePlanServiceResult.getServiceScheduleId(),
                carePlanServiceResult.getStartedAt(),
                carePlanServiceResult.getFinishedAt(),
                carePlanServiceResult.getNote()
        );
    }
}
