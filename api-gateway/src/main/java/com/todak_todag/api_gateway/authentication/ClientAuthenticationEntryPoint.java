package com.todak_todag.api_gateway.authentication;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import reactor.core.publisher.Mono;

@Component
public class ClientAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

	@Override
	public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
		return Mono.error(new TokenException(TokenErrorCode.UNAUTHORIZED, ex));
	}
	
}
