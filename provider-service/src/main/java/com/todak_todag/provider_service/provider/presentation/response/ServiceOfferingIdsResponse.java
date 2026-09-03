package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ServiceOfferingIdsResult;

import java.util.List;
import java.util.UUID;

public record ServiceOfferingIdsResponse(
        List<UUID> content
) {

    public static ServiceOfferingIdsResponse from(ServiceOfferingIdsResult result) {
        return new ServiceOfferingIdsResponse(result.serviceOfferingIds());
    }
}