package com.todak_todag.provider_service.provider.application.command;

import java.util.UUID;

public final class ServiceOfferingCommand {

    public record Create(
            UUID providerId,
            UUID provideServiceId
    ) {
    }
}