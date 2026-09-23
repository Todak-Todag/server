package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;

import java.util.UUID;

public record ServiceScheduleCompleteResponse(
        UUID serviceScheduleId,
        String status
) {

    public static ServiceScheduleCompleteResponse from(ServiceScheduleCompleteResult result) {
        return new ServiceScheduleCompleteResponse(result.serviceScheduleId(), result.status().name());
    }
}
