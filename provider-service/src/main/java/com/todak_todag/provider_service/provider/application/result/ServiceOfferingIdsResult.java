package com.todak_todag.provider_service.provider.application.result;

import java.util.List;
import java.util.UUID;

public record ServiceOfferingIdsResult(
        List<UUID> serviceOfferingIds
) {
}