package com.todak_todag.provider_service.provider.application.command;

import java.time.LocalTime;
import java.util.UUID;

public record ProvideWorkUpdateCommand(
        UUID serviceOfferingId,
        UUID provideWorkId,
        UUID providerId,
        Integer day,
        LocalTime startedAt,
        LocalTime finishedAt
) {
}