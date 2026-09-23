package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;

import java.util.UUID;

public record CarePlanServiceSelectResponse(
        UUID provideServiceId
) {
    public static CarePlanServiceSelectResponse from(
            CarePlanServiceSelectResult carePlanServiceSelectResult
    ) {
        return new CarePlanServiceSelectResponse(
                carePlanServiceSelectResult.provideServiceId()
        );
    }
}