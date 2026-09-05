package com.todak_todag.schedule_service.schedule.presentation.request;

import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResultRegisterRequest(
        @NotNull
        LocalDateTime startedAt,
        @NotNull
        LocalDateTime finishedAt,
        String note
) {

    public ServiceResultRegisterCommand toCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceResultRegisterCommand(serviceScheduleId, startedAt, finishedAt, note, requesterId);
    }
}
