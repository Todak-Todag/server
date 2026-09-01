package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProvideWorkPersistenceAdapter
        implements ProvideWorkCommandRepository, ProvideWorkQueryRepository {

    private final JpaProvideWorkRepository jpaProvideWorkRepository;

    @Override
    public ProvideWork save(ProvideWork provideWork) {
        return jpaProvideWorkRepository.save(provideWork);
    }

    @Override
    public Optional<ProvideWork> findById(UUID provideWorkId) {
        return jpaProvideWorkRepository.findById(provideWorkId);
    }

    @Override
    public List<ProvideWork> findAllByServiceOfferingId(UUID serviceOfferingId) {
        return jpaProvideWorkRepository.findAllByServiceOfferingId(serviceOfferingId);
    }
}