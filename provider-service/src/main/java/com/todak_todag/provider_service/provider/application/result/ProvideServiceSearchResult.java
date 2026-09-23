package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;

import java.time.Instant;
import java.util.UUID;

public record ProvideServiceSearchResult(
        UUID provideServiceId,
        String provideServiceName,
        String content,
        Instant createdAt
) {

    public static ProvideServiceSearchResult from(ProvideService provideService) {
        return new ProvideServiceSearchResult(
                provideService.getId(),
                provideService.getName(),
                provideService.getContent(),
                provideService.getCreatedAt()
        );
    }
}