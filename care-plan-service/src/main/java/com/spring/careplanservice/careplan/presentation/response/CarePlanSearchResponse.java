package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanSearchResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CarePlanSearchResponse(
        UUID carePlanId,
        CarePlanStatus status,
        LocalDate startDate,
        LocalDate finishDate,
        Instant createdAt
) {

    public static CarePlanSearchResponse from(
            CarePlanSearchResult carePlanSearchResult
    ) {
        return new CarePlanSearchResponse(
                carePlanSearchResult.carePlanId(),
                carePlanSearchResult.status(),
                carePlanSearchResult.startDate(),
                carePlanSearchResult.finishDate(),
                carePlanSearchResult.createdAt()
        );
    }
}