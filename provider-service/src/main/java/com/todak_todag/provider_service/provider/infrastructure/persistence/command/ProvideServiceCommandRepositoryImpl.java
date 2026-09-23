package com.todak_todag.provider_service.provider.infrastructure.persistence.command;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideServiceCommandRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProvideServiceCommandRepositoryImpl implements ProvideServiceCommandRepository {

    private final JpaProvideServiceRepository jpaProvideServiceRepository;

    @Override
    public ProvideService save(ProvideService provideService) {
        return jpaProvideServiceRepository.save(provideService);
    }
}
