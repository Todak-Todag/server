package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleDetailResult(
        UUID serviceScheduleId,
        UUID servicePreferenceId,
        UUID serviceOfferingId,
        ScheduleStatus status,
        LocalDate date,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String cancelReason,
        LocalDateTime canceledAt
) {

    public static ServiceScheduleDetailResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleDetailResult(
                serviceSchedule.getId(),
                serviceSchedule.getServicePreferenceId(),
                serviceSchedule.getServiceOfferingId(),
                serviceSchedule.getStatus(),
                serviceSchedule.getDate(),
                serviceSchedule.getStartedAt(),
                serviceSchedule.getFinishedAt(),
                serviceSchedule.getCancelReason(),
                serviceSchedule.getCanceledAt()
        );
    }
}
