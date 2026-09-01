package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingResult;

import java.time.Instant;
import java.util.UUID;

public final class ServiceOfferingResponse {

    public record Create(
            UUID serviceOfferingId,
            UUID providerId,
            Instant createdAt
    ) {
        public static Create from(ServiceOfferingResult.Create result) {
            return new Create(result.serviceOfferingId(), result.providerId(), result.createdAt());
        }
    }
}