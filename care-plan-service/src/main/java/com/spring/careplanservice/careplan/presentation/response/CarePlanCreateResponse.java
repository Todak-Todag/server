package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;

import java.util.UUID;

public record CarePlanCreateResponse(
        UUID carePlanId
) {
    public static CarePlanCreateResponse from(
            CarePlanCreateResult result
    ) {
        return new CarePlanCreateResponse(
                result.carePlanId()
        );
    }
}
