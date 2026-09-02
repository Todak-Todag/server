package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

import java.util.UUID;

// Facade가 CarePlanPort 호출에 필요한 servicePreferenceId를 얻기 위해 조회하는 결과
public record ServiceScheduleResult(
        UUID serviceScheduleId,
        UUID servicePreferenceId
) {

    public static ServiceScheduleResult from(ServiceSchedule serviceSchedule) {
        return new ServiceScheduleResult(serviceSchedule.getId(), serviceSchedule.getServicePreferenceId());
    }
}
