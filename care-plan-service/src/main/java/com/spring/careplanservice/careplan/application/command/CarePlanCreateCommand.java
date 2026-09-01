package com.spring.careplanservice.careplan.application.command;

import java.util.List;
import java.util.UUID;

public record CarePlanCreateCommand(
        UUID patientId,
        UUID dischargeId,
        String note,
        List<UUID> provideServiceIds
) {
}
