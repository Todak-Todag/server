package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProvideServiceQueryRepository {

    boolean existsById(UUID provideServiceId);

    boolean existsByName(String name);

    Page<ProvideService> findAll(Pageable pageable);

    List<ProvideService> findAllByIdIn(List<UUID> provideServiceIds);
}