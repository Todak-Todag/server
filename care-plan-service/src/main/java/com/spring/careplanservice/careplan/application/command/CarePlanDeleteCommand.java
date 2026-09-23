package com.spring.careplanservice.careplan.application.command;

import java.util.UUID;

public record CarePlanDeleteCommand(
        UUID userId,
        UUID carePlanId
) {
}
