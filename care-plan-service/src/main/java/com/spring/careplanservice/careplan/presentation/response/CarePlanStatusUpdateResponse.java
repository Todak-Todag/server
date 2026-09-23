package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanStatusUpdateResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.UUID;

public record CarePlanStatusUpdateResponse(
        UUID carePlanId,
        CarePlanStatus status
) {

    public static CarePlanStatusUpdateResponse from(
            CarePlanStatusUpdateResult carePlanStatusUpdateResult
    ) {
        return new CarePlanStatusUpdateResponse(
                carePlanStatusUpdateResult.carePlanId(),
                carePlanStatusUpdateResult.status()
        );
    }
}