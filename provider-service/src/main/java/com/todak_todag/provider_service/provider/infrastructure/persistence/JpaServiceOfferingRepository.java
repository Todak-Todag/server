package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);
}