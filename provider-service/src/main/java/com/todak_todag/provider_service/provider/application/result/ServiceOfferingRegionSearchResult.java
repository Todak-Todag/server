package com.todak_todag.provider_service.provider.application.result;

import java.util.UUID;

public record ServiceOfferingRegionSearchResult(
        UUID serviceOfferingId,
        UUID providerId,
        UUID provideServiceId,
        String provideServiceName
) {
}
