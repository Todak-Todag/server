package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;

import java.util.UUID;

// 03_서비스일정변경.md Response 표와 정확히 일치: serviceScheduleId, status
public record ServiceScheduleRescheduleResponse(
        UUID serviceScheduleId,
        String status
) {

    public static ServiceScheduleRescheduleResponse from(ServiceScheduleRescheduleResult result) {
        return new ServiceScheduleRescheduleResponse(
                result.serviceScheduleId(),
                result.status().name()
        );
    }
}
