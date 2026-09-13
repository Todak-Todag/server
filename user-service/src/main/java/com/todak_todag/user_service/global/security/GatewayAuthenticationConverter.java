package com.todak_todag.user_service.global.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

public class GatewayAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		
		UserContext user = UserContext.from(jwt.getSubject(), jwt.getClaimAsString("role"));
		
		if(user == null) {
			throw new BadJwtException("게이트웨이 토큰의 sub/role 클레임이 올바르지 않습니다.");
		}
		
		return new UsernamePasswordAuthenticationToken(
				user, jwt, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()))
		);
	}

}
