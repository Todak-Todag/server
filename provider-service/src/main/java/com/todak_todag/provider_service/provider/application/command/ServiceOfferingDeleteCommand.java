package com.todak_todag.provider_service.provider.application.command;

import com.todak_todag.provider_service.global.common.UserRole;

import java.util.UUID;

public record ServiceOfferingDeleteCommand(
        UUID serviceOfferingId,
        UUID userId,
        UserRole userRole
) {
}