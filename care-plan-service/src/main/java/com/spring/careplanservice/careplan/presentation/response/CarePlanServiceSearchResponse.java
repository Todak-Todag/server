package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanServiceSearchResult;

import java.time.Instant;
import java.util.UUID;

public record CarePlanServiceSearchResponse(
        UUID planServiceId,
        UUID provideServiceId,
        String provideServiceName,
        Instant createdAt
) {
    public static CarePlanServiceSearchResponse from(
            CarePlanServiceSearchResult carePlanServiceSearchResult
    ) {
        return new CarePlanServiceSearchResponse(
                carePlanServiceSearchResult.planServiceId(),
                carePlanServiceSearchResult.provideServiceId(),
                carePlanServiceSearchResult.provideServiceName(),
                carePlanServiceSearchResult.createdAt()
        );
    }
}
