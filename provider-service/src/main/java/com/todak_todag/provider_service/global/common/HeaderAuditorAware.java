package com.todak_todag.provider_service.global.common;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
public class HeaderAuditorAware implements AuditorAware<UUID> {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public Optional<UUID> getCurrentAuditor() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.of(SYSTEM_ID);
        }

        String userId = attributes.getRequest().getHeader(USER_ID_HEADER);

        if (userId == null || userId.isBlank()) {
            return Optional.of(SYSTEM_ID);
        }

        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            return Optional.of(SYSTEM_ID);
        }
    }
}