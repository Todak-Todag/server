package com.todak_todag.social_worker_service.global.common;

import com.todak_todag.social_worker_service.global.security.UserContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("springSecurityAuditorAware")
public class HeaderAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserContext user)) {
            return Optional.of(SystemId.SYSTEM_USER_ID);
        }

        return Optional.ofNullable(user.getUserId())
                .or(() -> Optional.of(SystemId.SYSTEM_USER_ID));
    }
}