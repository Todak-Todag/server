package com.todak_todag.api_gateway.authentication;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import reactor.core.publisher.Mono;

@Component
public class ClientAccessDeniedHandler implements ServerAccessDeniedHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
		return Mono.error(new TokenException(TokenErrorCode.ACCESS_DENIED, denied));
	}
	
}
