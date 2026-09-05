package com.spring.careplanservice.careplan.application.command;

import java.util.UUID;

public record ServicePreferenceDeleteCommand(
        UUID userId,
        UUID servicePreferenceId
) {
}
