package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleSearchResult(
        UUID serviceScheduleId,
        ScheduleStatus status,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ServiceScheduleSearchResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleSearchResult(
                serviceSchedule.getId(),
                serviceSchedule.getStatus(),
                serviceSchedule.getDate(),
                serviceSchedule.getStartedAt(),
                serviceSchedule.getFinishedAt()
        );
    }
}
