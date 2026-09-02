package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.InternalServiceScheduleSearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// [내부 API] 서비스 제공자 일정 조회 응답
public record InternalServiceScheduleResponse(
        UUID serviceScheduleId,
        UUID serviceOfferingId,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String status
) {
    public static InternalServiceScheduleResponse from(InternalServiceScheduleSearchResult result) {
        return new InternalServiceScheduleResponse(
                result.serviceScheduleId(),
                result.serviceOfferingId(),
                result.date(),
                result.startedAt(),
                result.finishedAt(),
                result.status().name()
        );
    }
}
