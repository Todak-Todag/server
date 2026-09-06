package com.todak_todag.schedule_service.schedule.application.event;

import java.time.LocalDate;
import java.util.UUID;

// ProviderReMatched 이벤트 페이로드
public record ProviderReMatchEvent(
        UUID serviceScheduleId,
        UUID serviceOfferingId,
        LocalDate newDate
) {
}
