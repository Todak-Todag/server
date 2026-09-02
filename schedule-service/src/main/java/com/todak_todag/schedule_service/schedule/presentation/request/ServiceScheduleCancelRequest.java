package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ServiceScheduleCancelRequest(
        @NotBlank
        String cancelReason
) {

    public ServiceScheduleCancelCommand toCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceScheduleCancelCommand(serviceScheduleId, cancelReason, requesterId);
    }
}
