package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;

import java.util.UUID;

public record ProvideWorkUpdateResponse(
        UUID provideWorkId
) {
    public static ProvideWorkUpdateResponse from(ProvideWorkUpdateResult result) {
        return new ProvideWorkUpdateResponse(result.provideWorkId());
    }
}