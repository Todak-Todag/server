package com.todak_todag.provider_service.provider.application.command;

import java.time.LocalTime;
import java.util.UUID;

public record ProvideWorkCreateCommand(
        UUID serviceOfferingId,
        UUID providerId,
        Integer day,
        LocalTime startedAt,
        LocalTime finishedAt
) {
}