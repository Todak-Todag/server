package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideServiceCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProvideServicePersistenceAdapter
        implements ProvideServiceQueryRepository, ProvideServiceCommandRepository {

    private final JpaProvideServiceRepository jpaProvideServiceRepository;

    @Override
    public ProvideService save(ProvideService provideService) {
        return jpaProvideServiceRepository.save(provideService);
    }

    @Override
    public boolean existsById(UUID provideServiceId) {
        return jpaProvideServiceRepository.existsById(provideServiceId);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaProvideServiceRepository.existsByName(name);
    }
}