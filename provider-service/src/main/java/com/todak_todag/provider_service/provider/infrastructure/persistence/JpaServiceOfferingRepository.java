package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);

    @Query("select s.id from ServiceOffering s where s.providerId = :providerId")
    List<UUID> findIdsByProviderId(@Param("providerId") UUID providerId);
}