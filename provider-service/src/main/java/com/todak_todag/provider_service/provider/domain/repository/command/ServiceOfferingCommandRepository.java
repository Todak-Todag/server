package com.todak_todag.provider_service.provider.domain.repository.command;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;

public interface ServiceOfferingCommandRepository {

    ServiceOffering save(ServiceOffering serviceOffering);
}