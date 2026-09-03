package com.todak_todag.provider_service.provider.application.result;

import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingSearchResult(
        UUID serviceOfferingId,
        UUID provideServiceId,
        String provideServiceName,
        Instant createdAt
) {
}