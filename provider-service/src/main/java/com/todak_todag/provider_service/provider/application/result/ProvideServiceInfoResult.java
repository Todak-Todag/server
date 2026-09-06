package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;

import java.util.UUID;

public record ProvideServiceInfoResult(
        UUID provideServiceId,
        String name,
        String content
) {

    public static ProvideServiceInfoResult from(ProvideService provideService) {
        return new ProvideServiceInfoResult(
                provideService.getId(),
                provideService.getName(),
                provideService.getContent()
        );
    }
}