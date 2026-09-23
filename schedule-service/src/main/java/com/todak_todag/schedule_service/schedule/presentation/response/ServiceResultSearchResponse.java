package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResultSearchResponse(
        UUID serviceResultId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ServiceResultSearchResponse from(ServiceResultSearchResult result) {
        return new ServiceResultSearchResponse(
                result.serviceResultId(),
                result.startedAt(),
                result.finishedAt()
        );
    }
}
