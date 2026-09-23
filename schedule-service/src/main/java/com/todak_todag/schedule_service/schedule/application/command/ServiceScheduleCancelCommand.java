package com.todak_todag.schedule_service.schedule.application.command;

import java.util.UUID;

// 서비스 일정 취소 요청
public record ServiceScheduleCancelCommand(
        UUID serviceScheduleId,
        String cancelReason,
        UUID requesterId
) {
}
