package com.todak_todag.provider_service.provider.domain.repository.command;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingCommandRepository {

    ServiceOffering save(ServiceOffering serviceOffering);

    // 제공 서비스와 하위 제공 가능 일정 쓰기를 직렬화하기 위해 부모 행을 잠근다
    Optional<ServiceOffering> findByIdForUpdate(UUID serviceOfferingId);
}