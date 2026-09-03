package com.todak_todag.provider_service.provider.domain.repository.query;

import java.time.Instant;
import java.util.UUID;

public record ServiceOfferingView(
        UUID serviceOfferingId,
        UUID provideServiceId,
        String provideServiceName,
        Instant createdAt
) {
}