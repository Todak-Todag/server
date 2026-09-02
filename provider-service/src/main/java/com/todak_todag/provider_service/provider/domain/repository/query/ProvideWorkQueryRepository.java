package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvideWorkQueryRepository {

    Optional<ProvideWork> findById(UUID provideWorkId);

    List<ProvideWork> findAllByServiceOfferingId(UUID serviceOfferingId);
}