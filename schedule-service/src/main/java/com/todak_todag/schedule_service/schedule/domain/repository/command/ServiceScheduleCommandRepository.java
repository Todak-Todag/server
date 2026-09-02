package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;

public interface ServiceScheduleCommandRepository {

    ServiceSchedule save(ServiceSchedule serviceSchedule);
}
