package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarePlanSearchResult(
        UUID carePlanId,
        CarePlanStatus status,
        LocalDate startDate,
        LocalDate finishDate,
        Instant createdAt
) {
    public static CarePlanSearchResult from(CarePlan carePlan) {
        return new CarePlanSearchResult(
                carePlan.getId(),
                carePlan.getStatus(),
                carePlan.getStartDate(),
                carePlan.getFinishDate(),
                carePlan.getCreatedAt()
        );
    }
}