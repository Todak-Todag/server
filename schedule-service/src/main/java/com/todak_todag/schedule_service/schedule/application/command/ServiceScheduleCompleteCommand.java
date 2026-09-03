package com.todak_todag.schedule_service.schedule.application.command;

import java.util.UUID;

public record ServiceScheduleCompleteCommand(
        UUID serviceScheduleId,
        ServiceScheduleCompletionStatus status,
        UUID requesterId
) {
}
