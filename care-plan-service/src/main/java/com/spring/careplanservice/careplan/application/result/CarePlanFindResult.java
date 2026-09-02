package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarePlanFindResult(
        UUID carePlanId,
        UUID patientId,
        UUID dischargeId,
        CarePlanStatus status,
        LocalDate startDate,
        LocalDate finishDate,
        String note,
        Instant createdAt
) {

    public static CarePlanFindResult from(
            CarePlan carePlan
    ) {
        return new CarePlanFindResult(
                carePlan.getId(),
                carePlan.getPatientId(),
                carePlan.getDischargeId(),
                carePlan.getStatus(),
                carePlan.getStartDate(),
                carePlan.getFinishDate(),
                carePlan.getNote(),
                carePlan.getCreatedAt()
        );
    }
}