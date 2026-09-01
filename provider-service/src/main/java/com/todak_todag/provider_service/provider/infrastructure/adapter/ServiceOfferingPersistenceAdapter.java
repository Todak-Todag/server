package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceOfferingPersistenceAdapter
        implements ServiceOfferingCommandRepository, ServiceOfferingQueryRepository {

    private final JpaServiceOfferingRepository jpaServiceOfferingRepository;

    @Override
    public ServiceOffering save(ServiceOffering serviceOffering) {
        return jpaServiceOfferingRepository.save(serviceOffering);
    }

    @Override
    public Optional<ServiceOffering> findById(UUID serviceOfferingId) {
        return jpaServiceOfferingRepository.findById(serviceOfferingId);
    }

    @Override
    public boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId) {
        return jpaServiceOfferingRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId);
    }
}