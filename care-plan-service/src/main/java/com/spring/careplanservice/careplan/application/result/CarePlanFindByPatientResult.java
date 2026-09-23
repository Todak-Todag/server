package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.util.UUID;

public record CarePlanFindByPatientResult(
        UUID carePlanId,
        UUID patientId,
        CarePlanStatus status
) {
    public static CarePlanFindByPatientResult from(
            CarePlan carePlan
    ) {
        return new CarePlanFindByPatientResult(
                carePlan.getId(),
                carePlan.getPatientId(),
                carePlan.getStatus()
        );
    }
}
