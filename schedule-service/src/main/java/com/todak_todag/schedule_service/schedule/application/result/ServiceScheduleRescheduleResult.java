package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.UUID;

public record ServiceScheduleRescheduleResult(
        UUID serviceScheduleId,
        ScheduleStatus status
) {

    public static ServiceScheduleRescheduleResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleRescheduleResult(
                serviceSchedule.getId(),
                serviceSchedule.getStatus()
        );
    }
}
