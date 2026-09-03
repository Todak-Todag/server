package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.UUID;

public record ServiceScheduleResult(
        UUID serviceScheduleId,
        UUID servicePreferenceId,
        UUID serviceOfferingId
) {

    public static ServiceScheduleResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleResult(
                serviceSchedule.getId(),
                serviceSchedule.getServicePreferenceId(),
                serviceSchedule.getServiceOfferingId()
        );
    }
}
