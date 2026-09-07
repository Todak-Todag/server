package com.todak_todag.provider_service.provider.application.port;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// 제공자가 이미 배정받은 시간 구간
public record ScheduleSlot(
        UUID serviceOfferingId,
        LocalDate date,
        LocalTime startedAt,
        LocalTime finishedAt
) {
}