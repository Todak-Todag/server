package com.todak_todag.schedule_service.schedule.application.command;

import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;

import java.time.LocalDate;
import java.util.UUID;

// 재매칭 시도 요청
public record MatchingAttemptRetryCommand(
        UUID matchingAttemptId,
        LocalDate date,
        PreferredTimeSlot preferredTimeSlot,
        UUID requesterId
) {
}
