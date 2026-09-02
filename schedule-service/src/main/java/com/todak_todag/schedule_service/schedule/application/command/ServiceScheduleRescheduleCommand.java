package com.todak_todag.schedule_service.schedule.application.command;

import java.time.LocalDate;
import java.util.UUID;

// 서비스 일정 변경 요청
public record ServiceScheduleRescheduleCommand(
        UUID serviceScheduleId,
        LocalDate date,
        UUID requesterId
) {
}
