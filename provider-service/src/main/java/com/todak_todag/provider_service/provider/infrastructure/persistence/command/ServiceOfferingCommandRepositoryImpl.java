package com.todak_todag.provider_service.provider.infrastructure.persistence.command;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceOfferingCommandRepositoryImpl implements ServiceOfferingCommandRepository {

    private final JpaServiceOfferingRepository jpaServiceOfferingRepository;

    @Override
    public ServiceOffering save(ServiceOffering serviceOffering) {
        return jpaServiceOfferingRepository.save(serviceOffering);
    }
}
