package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleCancelResponse(
        UUID serviceScheduleId,
        LocalDateTime canceledAt
) {

    public static ServiceScheduleCancelResponse from(ServiceScheduleCancelResult result) {
        return new ServiceScheduleCancelResponse(result.serviceScheduleId(), result.canceledAt());
    }
}
