package com.spring.careplanservice.careplan.application.command;

import java.util.UUID;

public record CarePlanServiceSelectCommand(
        UUID userId,
        UUID carePlanId,
        UUID provideServiceId
) {
}
