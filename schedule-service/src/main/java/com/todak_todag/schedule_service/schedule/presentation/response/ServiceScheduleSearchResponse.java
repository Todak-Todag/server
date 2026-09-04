package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleSearchResponse(
        UUID serviceScheduleId,
        String status,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ServiceScheduleSearchResponse from(ServiceScheduleSearchResult result) {
        return new ServiceScheduleSearchResponse(
                result.serviceScheduleId(),
                result.status().name(),
                result.date(),
                result.startedAt(),
                result.finishedAt()
        );
    }
}
