package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.UUID;

public record ServiceScheduleCompleteResult(
        UUID serviceScheduleId,
        ScheduleStatus status
) {

    public static ServiceScheduleCompleteResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleCompleteResult(serviceSchedule.getId(), serviceSchedule.getStatus());
    }
}
