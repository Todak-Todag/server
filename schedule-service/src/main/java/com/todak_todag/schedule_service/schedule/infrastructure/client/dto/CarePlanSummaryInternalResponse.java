package com.todak_todag.schedule_service.schedule.infrastructure.client.dto;

import java.util.UUID;

public record CarePlanSummaryInternalResponse(
        UUID carePlanId,
        UUID patientId,
        String status
) {
}
