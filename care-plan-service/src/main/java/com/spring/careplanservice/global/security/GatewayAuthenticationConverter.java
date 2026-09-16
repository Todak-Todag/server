package com.spring.careplanservice.global.security;

import com.spring.careplanservice.global.common.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public class GatewayAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String roleClaim = jwt.getClaimAsString("role");

            if (roleClaim == null) {
                throw new BadJwtException(
                        "게이트웨이 토큰의 sub/role 클레임이 올바르지 않습니다."
                );
            }

            UserRole userRole = UserRole.valueOf(roleClaim);

            UserContext userContext = new UserContext(userId, userRole);

            return new UsernamePasswordAuthenticationToken(
                    userContext,
                    jwt,
                    List.of(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + userRole.name()
                            )
                    )
            );

        } catch (IllegalArgumentException e) {
            throw new BadJwtException(
                    "게이트웨이 토큰의 sub/role 클레임이 올바르지 않습니다."
            );
        }
    }
}
