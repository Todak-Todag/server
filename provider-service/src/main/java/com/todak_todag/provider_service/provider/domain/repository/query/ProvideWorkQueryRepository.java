package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvideWorkQueryRepository {

    Optional<ProvideWork> findById(UUID provideWorkId);

    List<ProvideWork> findAllByServiceOfferingId(UUID serviceOfferingId);

    List<ProvideWork> findAllByServiceOfferingIdIn(List<UUID> serviceOfferingIds);

    boolean existsOverlapped(
            UUID serviceOfferingId,
            UUID excludedProvideWorkId,
            Integer day,
            LocalTime startedAt,
            LocalTime finishedAt
    );
}