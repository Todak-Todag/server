package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOutboxEventRepository extends JpaRepository<ProviderOutboxEvent, UUID> {

    List<ProviderOutboxEvent> findAllByPublishedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}