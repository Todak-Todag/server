package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceResultDetailResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResultDetailResponse(
        UUID serviceResultId,
        UUID serviceScheduleId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String note
) {

    public static ServiceResultDetailResponse from(ServiceResultDetailResult result) {
        return new ServiceResultDetailResponse(
                result.serviceResultId(),
                result.serviceScheduleId(),
                result.startedAt(),
                result.finishedAt(),
                result.note()
        );
    }
}
