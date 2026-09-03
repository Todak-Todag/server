package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceScheduleCancelResult(
        UUID serviceScheduleId,
        LocalDateTime canceledAt
) {

    public static ServiceScheduleCancelResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleCancelResult(
                serviceSchedule.getId(),
                serviceSchedule.getCanceledAt()
        );
    }
}
