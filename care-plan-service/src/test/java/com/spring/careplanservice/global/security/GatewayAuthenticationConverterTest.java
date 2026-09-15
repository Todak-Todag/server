package com.spring.careplanservice.global.security;

import com.spring.careplanservice.global.common.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayAuthenticationConverterTest {
    private final GatewayAuthenticationConverter converter = new GatewayAuthenticationConverter();

    @Test
    void 정상_JWT를_UserContext로_변환한다() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("role", "PATIENT")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        UserContext principal = (UserContext) authentication.getPrincipal();

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo(UserRole.PATIENT);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PATIENT");
    }

    @Test
    void 잘못된_sub는_인증에_실패한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("invalid-uuid")
                .claim("role", "PATIENT")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void 잘못된_role은_인증에_실패한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("role", "INVALID_ROLE")
                .build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(BadJwtException.class);
    }
}