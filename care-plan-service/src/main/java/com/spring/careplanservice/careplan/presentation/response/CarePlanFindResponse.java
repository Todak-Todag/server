package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarePlanFindResponse(
        UUID carePlanId,
        UUID patientId,
        UUID dischargeId,
        CarePlanStatus status,
        LocalDate startDate,
        LocalDate finishDate,
        String note,
        Instant createdAt
) {
    public static CarePlanFindResponse from(
            CarePlanFindResult carePlanFindResult
    ) {
        return new CarePlanFindResponse(
                carePlanFindResult.carePlanId(),
                carePlanFindResult.patientId(),
                carePlanFindResult.dischargeId(),
                carePlanFindResult.status(),
                carePlanFindResult.startDate(),
                carePlanFindResult.finishDate(),
                carePlanFindResult.note(),
                carePlanFindResult.createdAt()
        );
    }
}