package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;

import java.util.UUID;

public record ServiceResultRegisterResponse(
        UUID serviceResultId
) {

    public static ServiceResultRegisterResponse from(ServiceResultRegisterResult result) {
        return new ServiceResultRegisterResponse(result.serviceResultId());
    }
}
