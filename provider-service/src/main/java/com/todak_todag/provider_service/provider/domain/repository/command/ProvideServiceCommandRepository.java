package com.todak_todag.provider_service.provider.domain.repository.command;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;

public interface ProvideServiceCommandRepository {

    ProvideService save(ProvideService provideService);
}