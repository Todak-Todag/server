package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;

import java.util.UUID;

public record ServiceOfferingRegionSearchResponse(
        UUID serviceOfferingId,
        UUID providerId,
        UUID provideServiceId,
        String provideServiceName
) {
    public static ServiceOfferingRegionSearchResponse from(ServiceOfferingRegionSearchResult result) {
        return new ServiceOfferingRegionSearchResponse(
                result.serviceOfferingId(),
                result.providerId(),
                result.provideServiceId(),
                result.provideServiceName()
        );
    }
}
