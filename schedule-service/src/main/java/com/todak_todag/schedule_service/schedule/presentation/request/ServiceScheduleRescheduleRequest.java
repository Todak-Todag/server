package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

// 03_서비스일정변경.md Request 스펙: date(Body, Date, 필수)
public record ServiceScheduleRescheduleRequest(
        @NotNull
        LocalDate date
) {

    public ServiceScheduleRescheduleCommand toCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceScheduleRescheduleCommand(serviceScheduleId, date, requesterId);
    }
}
