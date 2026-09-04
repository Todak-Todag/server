package com.todak_todag.provider_service.provider.infrastructure.persistence.query;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProvideServiceQueryRepositoryImpl implements ProvideServiceQueryRepository {

    private final JpaProvideServiceRepository jpaProvideServiceRepository;

    @Override
    public boolean existsById(UUID provideServiceId) {
        return jpaProvideServiceRepository.existsById(provideServiceId);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaProvideServiceRepository.existsByName(name);
    }

    @Override
    public Page<ProvideService> findAll(Pageable pageable) {
        return jpaProvideServiceRepository.findAll(pageable);
    }
}
