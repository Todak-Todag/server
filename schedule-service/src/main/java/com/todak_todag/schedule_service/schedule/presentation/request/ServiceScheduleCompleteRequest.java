package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompletionStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServiceScheduleCompleteRequest(
        @NotNull
        ServiceScheduleCompletionStatus status
) {

    public ServiceScheduleCompleteCommand toCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceScheduleCompleteCommand(serviceScheduleId, status, requesterId);
    }
}
