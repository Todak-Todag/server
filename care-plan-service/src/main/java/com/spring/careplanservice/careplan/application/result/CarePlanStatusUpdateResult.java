package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.UUID;

public record CarePlanStatusUpdateResult(
        UUID carePlanId,
        CarePlanStatus status
) {

    public static CarePlanStatusUpdateResult from(
            CarePlan carePlan
    ) {
        return new CarePlanStatusUpdateResult(
                carePlan.getId(),
                carePlan.getStatus()
        );
    }
}