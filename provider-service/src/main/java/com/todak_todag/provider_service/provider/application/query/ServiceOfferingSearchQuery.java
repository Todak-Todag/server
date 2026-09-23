package com.todak_todag.provider_service.provider.application.query;

import com.todak_todag.provider_service.global.common.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record ServiceOfferingSearchQuery(
        UUID providerId,
        UUID userId,
        UserRole userRole,
        Pageable pageable
) {
}