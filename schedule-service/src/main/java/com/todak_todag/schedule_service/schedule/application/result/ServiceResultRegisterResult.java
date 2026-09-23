package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.util.UUID;

public record ServiceResultRegisterResult(
        UUID serviceResultId
) {

    public static ServiceResultRegisterResult from(CarePlanServiceResult carePlanServiceResult) {
        return new ServiceResultRegisterResult(carePlanServiceResult.getServiceResultId());
    }
}
