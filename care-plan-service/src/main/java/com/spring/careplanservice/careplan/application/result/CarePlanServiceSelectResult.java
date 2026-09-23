package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

import java.util.UUID;

public record CarePlanServiceSelectResult(
        UUID provideServiceId
) {
    public static CarePlanServiceSelectResult from(
            CarePlanService carePlanService
    ) {
        return new CarePlanServiceSelectResult(
                carePlanService.getProvideServiceId()
        );
    }
}