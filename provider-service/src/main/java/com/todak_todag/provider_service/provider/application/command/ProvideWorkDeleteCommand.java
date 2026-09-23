package com.todak_todag.provider_service.provider.application.command;

import java.util.UUID;

public record ProvideWorkDeleteCommand(
        UUID serviceOfferingId,
        UUID provideWorkId,
        UUID providerId
) {
}