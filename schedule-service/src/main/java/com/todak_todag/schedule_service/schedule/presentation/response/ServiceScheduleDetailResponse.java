package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleDetailResponse(
        UUID serviceScheduleId,
        UUID servicePreferenceId,
        UUID serviceOfferingId,
        String status,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String cancelReason,
        LocalDateTime canceledAt
) {

    public static ServiceScheduleDetailResponse from(ServiceScheduleDetailResult result) {
        return new ServiceScheduleDetailResponse(
                result.serviceScheduleId(),
                result.servicePreferenceId(),
                result.serviceOfferingId(),
                result.status().name(),
                result.date(),
                result.startedAt(),
                result.finishedAt(),
                result.cancelReason(),
                result.canceledAt()
        );
    }
}
