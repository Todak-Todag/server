package com.todak_todag.provider_service.provider.domain.repository.query;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingQueryRepository {

    Optional<ServiceOffering> findById(UUID serviceOfferingId);

    // 과거 일정 참조용 — 논리 삭제된 제공 서비스도 포함한다
    Optional<UUID> findProviderIdIncludingDeleted(UUID serviceOfferingId);

    // 과거 일정 참조용 — 논리 삭제된 제공 서비스도 포함한다
    List<UUID> findIdsByProviderIdIncludingDeleted(UUID providerId);

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);

    Page<ServiceOfferingView> searchByProviderId(UUID providerId, Pageable pageable);

    Page<ServiceOfferingView> searchByRegionId(UUID regionId, Pageable pageable);

    // 매칭 후보 조회 — 지역과 서비스 종류가 모두 일치해야 한다
    List<ServiceOffering> findAllByRegionIdAndProvideServiceId(UUID regionId, UUID provideServiceId);
}
