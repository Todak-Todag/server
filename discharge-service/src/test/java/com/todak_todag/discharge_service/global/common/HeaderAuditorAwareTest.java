package com.todak_todag.discharge_service.global.common;

import com.todak_todag.discharge_service.global.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAuditorAwareTest {

    private final HeaderAuditorAware auditorAware =
            new HeaderAuditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_사용자가_있으면_userId를_반환한다() {
        UUID userId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "HOSPITAL_STAFF"
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        java.util.Collections.emptyList()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThat(auditorAware.getCurrentAuditor())
                .contains(userId);
    }

    @Test
    void 인증정보가_없으면_SYSTEM_ID를_반환한다() {
        UUID systemId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000000"
                );

        assertThat(auditorAware.getCurrentAuditor())
                .contains(systemId);
    }
}