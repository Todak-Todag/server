package com.todak_todag.provider_service.provider.application.command;

import java.util.UUID;

public record ServiceOfferingCreateCommand(
        UUID providerId,
        UUID provideServiceId
) {
}