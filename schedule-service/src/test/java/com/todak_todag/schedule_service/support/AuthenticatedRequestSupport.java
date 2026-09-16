package com.todak_todag.schedule_service.support;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.security.UserContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public class AuthenticatedRequestSupport {

    private AuthenticatedRequestSupport() {}

    public static RequestPostProcessor asUser(UUID userId, UserRole role) {
        UserContext user = UserContext.from(userId.toString(), role.name());

        return authentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        ));
    }
}
