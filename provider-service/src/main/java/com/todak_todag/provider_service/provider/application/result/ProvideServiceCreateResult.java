package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;

import java.util.UUID;

public record ProvideServiceCreateResult(
        UUID provideServiceId,
        String name,
        String content
) {

    public static ProvideServiceCreateResult from(ProvideService provideService) {
        return new ProvideServiceCreateResult(
                provideService.getId(),
                provideService.getName(),
                provideService.getContent()
        );
    }
}