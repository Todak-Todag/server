package com.spring.careplanservice.careplan.application.command;

import java.util.UUID;

public record CarePlanServiceCancelCommand(
        UUID userId,
        UUID planServiceId
) {
}
