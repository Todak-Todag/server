package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProvideServicePersistenceAdapter implements ProvideServiceQueryRepository {

    private final JpaProvideServiceRepository jpaProvideServiceRepository;

    @Override
    public boolean existsById(UUID provideServiceId) {
        return jpaProvideServiceRepository.existsById(provideServiceId);
    }
}