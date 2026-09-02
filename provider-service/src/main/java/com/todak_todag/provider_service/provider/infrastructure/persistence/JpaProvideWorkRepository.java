package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProvideWorkRepository extends JpaRepository<ProvideWork, UUID> {

    List<ProvideWork> findAllByServiceOfferingId(UUID serviceOfferingId);
}