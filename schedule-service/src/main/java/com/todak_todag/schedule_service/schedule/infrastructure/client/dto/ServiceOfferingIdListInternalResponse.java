package com.todak_todag.schedule_service.schedule.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

public record ServiceOfferingIdListInternalResponse(
        List<UUID> content
) {
}
