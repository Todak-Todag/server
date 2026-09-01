package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;

import java.util.Optional;
import java.util.UUID;

public interface ProvideServiceQueryRepository {

    Optional<ProvideService> findById(UUID provideServiceId);
}