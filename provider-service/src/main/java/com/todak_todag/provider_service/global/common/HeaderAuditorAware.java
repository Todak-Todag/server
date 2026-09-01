package com.todak_todag.provider_service.global.common;

import com.todak_todag.provider_service.global.security.UserContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class HeaderAuditorAware implements AuditorAware<UUID> {

    private static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserContext user)) {
            return Optional.of(SYSTEM_ID);
        }

        return Optional.ofNullable(user.getUserId())
                .or(() -> Optional.of(SYSTEM_ID));
    }
}