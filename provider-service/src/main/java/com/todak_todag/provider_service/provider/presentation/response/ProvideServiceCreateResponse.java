package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;

import java.util.UUID;

public record ProvideServiceCreateResponse(
        UUID provideServiceId,
        String name,
        String content
) {

    public static ProvideServiceCreateResponse from(ProvideServiceCreateResult result) {
        return new ProvideServiceCreateResponse(
                result.provideServiceId(),
                result.name(),
                result.content()
        );
    }
}