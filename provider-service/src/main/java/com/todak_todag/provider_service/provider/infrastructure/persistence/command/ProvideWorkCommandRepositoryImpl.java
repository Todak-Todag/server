package com.todak_todag.provider_service.provider.infrastructure.persistence.command;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProvideWorkCommandRepositoryImpl implements ProvideWorkCommandRepository {

    private final JpaProvideWorkRepository jpaProvideWorkRepository;

    @Override
    public ProvideWork save(ProvideWork provideWork) {
        return jpaProvideWorkRepository.save(provideWork);
    }
}
