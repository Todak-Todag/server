package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.util.UUID;

public interface CarePlanServiceResultCommandRepository {

    CarePlanServiceResult save(CarePlanServiceResult carePlanServiceResult);

    boolean existsByServiceScheduleId(UUID serviceScheduleId);
}
