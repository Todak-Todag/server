package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;

import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingCreateResult(
        UUID serviceOfferingId,
        UUID providerId,
        Instant createdAt
) {
    public static ServiceOfferingCreateResult from(ServiceOffering serviceOffering) {
        return new ServiceOfferingCreateResult(
                serviceOffering.getId(),
                serviceOffering.getProviderId(),
                serviceOffering.getCreatedAt()
        );
    }
}