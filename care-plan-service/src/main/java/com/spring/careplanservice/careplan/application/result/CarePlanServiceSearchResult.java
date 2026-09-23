package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;

import java.time.Instant;
import java.util.UUID;

public record CarePlanServiceSearchResult(
        UUID planServiceId,
        UUID provideServiceId,
        String provideServiceName,
        Instant createdAt
) {
    public static CarePlanServiceSearchResult of(
            CarePlanService carePlanService,
            ProvideServiceInfoResult provideServiceInfo
    ) {
        return new CarePlanServiceSearchResult(
                carePlanService.getId(),
                carePlanService.getProvideServiceId(),
                provideServiceInfo != null ? provideServiceInfo.name() : null,
                carePlanService.getCreatedAt()
        );
    }
}
