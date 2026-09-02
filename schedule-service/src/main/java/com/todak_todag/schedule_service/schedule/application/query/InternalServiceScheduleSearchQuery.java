package com.todak_todag.schedule_service.schedule.application.query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// [내부 API] 서비스 제공자 일정 조회 요청 파라미터
public record InternalServiceScheduleSearchQuery(
        List<UUID> serviceOfferingIds,
        LocalDate startDate
) {
}
