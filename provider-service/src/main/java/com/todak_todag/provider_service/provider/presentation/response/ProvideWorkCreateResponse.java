package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;

import java.util.UUID;

public record ProvideWorkCreateResponse(
        UUID provideWorkId
) {
    public static ProvideWorkCreateResponse from(ProvideWorkCreateResult result) {
        return new ProvideWorkCreateResponse(result.provideWorkId());
    }
}