package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;

import java.util.UUID;

public record ProvideWorkUpdateResult(
        UUID provideWorkId
) {
    public static ProvideWorkUpdateResult from(ProvideWork provideWork) {
        return new ProvideWorkUpdateResult(provideWork.getId());
    }
}