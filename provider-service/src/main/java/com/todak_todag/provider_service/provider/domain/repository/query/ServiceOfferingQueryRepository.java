package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingQueryRepository {

    Optional<ServiceOffering> findById(UUID serviceOfferingId);

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);

    Page<ServiceOfferingView> searchByProviderId(UUID providerId, Pageable pageable);

    List<UUID> findIdsByProviderId(UUID providerId);

    Page<ServiceOfferingView> searchByRegionId(UUID regionId, Pageable pageable);
}
