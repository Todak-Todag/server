package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingProviderResult;

import java.util.UUID;

public record ServiceOfferingProviderResponse(
        UUID providerId
) {
    public static ServiceOfferingProviderResponse from(ServiceOfferingProviderResult result) {
        return new ServiceOfferingProviderResponse(result.providerId());
    }
}