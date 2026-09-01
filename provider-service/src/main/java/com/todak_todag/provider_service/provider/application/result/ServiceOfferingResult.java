package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;

import java.time.Instant;
import java.util.UUID;

public final class ServiceOfferingResult {

    public record Create(
            UUID serviceOfferingId,
            UUID providerId,
            Instant createdAt
    ) {
        public static Create from(ServiceOffering serviceOffering) {
            return new Create(
                    serviceOffering.getId(),
                    serviceOffering.getProviderId(),
                    serviceOffering.getCreatedAt()
            );
        }
    }
}