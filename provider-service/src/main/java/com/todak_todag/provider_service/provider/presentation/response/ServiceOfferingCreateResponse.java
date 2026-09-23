package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;

import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingCreateResponse(
        UUID serviceOfferingId,
        UUID providerId,
        Instant createdAt
) {
    public static ServiceOfferingCreateResponse from(ServiceOfferingCreateResult result) {
        return new ServiceOfferingCreateResponse(
                result.serviceOfferingId(),
                result.providerId(),
                result.createdAt()
        );
    }
}