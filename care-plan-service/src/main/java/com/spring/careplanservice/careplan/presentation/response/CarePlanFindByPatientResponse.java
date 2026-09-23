package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.UUID;

public record CarePlanFindByPatientResponse(
        UUID carePlanId,
        UUID patientId,
        CarePlanStatus status
) {

    public static CarePlanFindByPatientResponse from(
            CarePlanFindByPatientResult carePlanFindByPatientResult
    ) {
        return new CarePlanFindByPatientResponse(
                carePlanFindByPatientResult.carePlanId(),
                carePlanFindByPatientResult.patientId(),
                carePlanFindByPatientResult.status()
        );
    }
}