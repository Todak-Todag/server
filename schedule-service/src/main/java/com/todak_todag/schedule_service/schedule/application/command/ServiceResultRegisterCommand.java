package com.todak_todag.schedule_service.schedule.application.command;

import java.time.LocalDateTime;
import java.util.UUID;

// 서비스 수행 결과 등록
public record ServiceResultRegisterCommand(
        UUID serviceScheduleId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String note,
        UUID requesterId
) {
}
