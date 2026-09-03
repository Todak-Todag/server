package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingView;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.ServiceOfferingQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceOfferingPersistenceAdapter
        implements ServiceOfferingCommandRepository, ServiceOfferingQueryRepository {

    private final JpaServiceOfferingRepository jpaServiceOfferingRepository;
    private final ServiceOfferingQueryDslRepository serviceOfferingQueryDslRepository;

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

    @Override
    public Page<ServiceOfferingView> searchByProviderId(UUID providerId, Pageable pageable) {
        return serviceOfferingQueryDslRepository.searchByProviderId(providerId, pageable);
    }
}