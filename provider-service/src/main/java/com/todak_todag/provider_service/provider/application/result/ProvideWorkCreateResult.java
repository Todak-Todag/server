package com.todak_todag.provider_service.provider.application.result;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;

import java.util.UUID;

public record ProvideWorkCreateResult(
        UUID provideWorkId
) {
    public static ProvideWorkCreateResult from(ProvideWork provideWork) {
        return new ProvideWorkCreateResult(provideWork.getId());
    }
}