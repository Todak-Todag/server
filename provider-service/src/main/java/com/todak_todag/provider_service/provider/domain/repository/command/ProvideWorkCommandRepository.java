package com.todak_todag.provider_service.provider.domain.repository.command;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;

public interface ProvideWorkCommandRepository {

    ProvideWork save(ProvideWork provideWork);
}