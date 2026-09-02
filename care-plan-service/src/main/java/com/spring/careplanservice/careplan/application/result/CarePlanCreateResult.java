package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;

import java.util.UUID;

public record CarePlanCreateResult(
        UUID carePlanId
) {
    public static CarePlanCreateResult from(CarePlan carePlan) {
        return new CarePlanCreateResult(
                carePlan.getId()
        );
    }
}