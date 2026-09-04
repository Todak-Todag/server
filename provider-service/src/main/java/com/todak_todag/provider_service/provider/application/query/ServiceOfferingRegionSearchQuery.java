package com.todak_todag.provider_service.provider.application.query;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record ServiceOfferingRegionSearchQuery(
        UUID regionId,
        UUID userId,
        Pageable pageable
) {
}
