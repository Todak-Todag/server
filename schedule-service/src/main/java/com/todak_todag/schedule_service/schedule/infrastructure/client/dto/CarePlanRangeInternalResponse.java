package com.todak_todag.schedule_service.schedule.infrastructure.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CarePlanRangeInternalResponse(
        UUID carePlanId,
        LocalDate finishDate,
        UUID patientId
) {
}
