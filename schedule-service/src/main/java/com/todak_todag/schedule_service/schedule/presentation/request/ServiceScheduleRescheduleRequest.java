package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ServiceScheduleRescheduleRequest(
        @NotNull
        LocalDate date
) {

    public ServiceScheduleRescheduleCommand toCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceScheduleRescheduleCommand(serviceScheduleId, date, requesterId);
    }
}
