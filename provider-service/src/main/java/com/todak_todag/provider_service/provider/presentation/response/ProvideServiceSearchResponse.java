package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;

import java.time.Instant;
import java.util.UUID;

public record ProvideServiceSearchResponse(
        UUID provideServiceId,
        String provideServiceName,
        String content,
        Instant createdAt
) {

    public static ProvideServiceSearchResponse from(ProvideServiceSearchResult result) {
        return new ProvideServiceSearchResponse(
                result.provideServiceId(),
                result.provideServiceName(),
                result.content(),
                result.createdAt()
        );
    }
}