package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// [내부 API] 서비스 제공자 일정 조회 요청 결과
public record InternalServiceScheduleSearchResult(
        UUID serviceScheduleId,
        UUID serviceOfferingId,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        ScheduleStatus status
) {

    public static InternalServiceScheduleSearchResult from(ServiceSchedule serviceSchedule) {
        return new InternalServiceScheduleSearchResult(
                serviceSchedule.getId(),
                serviceSchedule.getServiceOfferingId(),
                serviceSchedule.getDate(),
                serviceSchedule.getStartedAt(),
                serviceSchedule.getFinishedAt(),
                serviceSchedule.getStatus()
        );
    }
}
