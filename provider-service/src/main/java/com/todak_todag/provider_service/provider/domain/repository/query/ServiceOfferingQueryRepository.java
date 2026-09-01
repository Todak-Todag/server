package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingQueryRepository {

    Optional<ServiceOffering> findById(UUID serviceOfferingId);

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);
}