package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;

import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingSearchResponse(
        UUID serviceOfferingId,
        UUID provideServiceId,
        String provideServiceName,
        Instant createdAt
) {
    public static ServiceOfferingSearchResponse from(ServiceOfferingSearchResult result) {
        return new ServiceOfferingSearchResponse(
                result.serviceOfferingId(),
                result.provideServiceId(),
                result.provideServiceName(),
                result.createdAt()
        );
    }
}