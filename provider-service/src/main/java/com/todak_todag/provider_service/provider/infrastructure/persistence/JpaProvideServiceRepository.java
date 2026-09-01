package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaProvideServiceRepository extends JpaRepository<ProvideService, UUID> {
}