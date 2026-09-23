package com.todak_todag.schedule_service.schedule.application.query;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

// 서비스 일정 목록 조회 요청 파라미터
public record ServiceScheduleSearchQuery(
        UUID userId,
        UserRole role,
        ScheduleStatus status,
        LocalDate date,
        Pageable pageable
) {
}
