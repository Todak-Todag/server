package com.todak_todag.schedule_service.global.security;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.common.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityAuditorAwareTest {

    private final SpringSecurityAuditorAware springSecurityAuditorAware = new SpringSecurityAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증_정보가_없으면_SYSTEM_USER_ID를_반환한다() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<UUID> auditor = springSecurityAuditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).contains(SystemId.SYSTEM_USER_ID);
    }

    @Test
    void 인증된_UserContext가_있으면_해당_userId를_반환한다() {
        // given
        UserContext userContext = UserContext.from(UUID.randomUUID().toString(), UserRole.PATIENT.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userContext, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")))
        );

        // when
        Optional<UUID> auditor = springSecurityAuditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).contains(userContext.getUserId());
    }

    @Test
    void principal이_UserContext가_아니면_SYSTEM_USER_ID를_반환한다() {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );

        // when
        Optional<UUID> auditor = springSecurityAuditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).contains(SystemId.SYSTEM_USER_ID);
    }
}
