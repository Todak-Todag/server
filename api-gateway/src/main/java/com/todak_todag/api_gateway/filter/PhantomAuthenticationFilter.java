package com.todak_todag.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.authentication.ClientContext;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PhantomAuthenticationFilter implements GlobalFilter, Ordered {

  private static final String USER_ID_HEADER = "X-User-Id";
	
	private static final String USER_ROLE_HEADER = "X-User-Role";
	
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerWebExchange sanitizedExchange = removeClientHeaders(exchange);
		
		return sanitizedExchange.getPrincipal()
				.filter(Authentication.class::isInstance)
				.cast(Authentication.class)
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getPrincipal)
				.filter(ClientContext.class::isInstance)
				.cast(ClientContext.class)
				.map(clientContext -> addClientHeaders(sanitizedExchange, clientContext))
				.defaultIfEmpty(sanitizedExchange)
				.flatMap(chain::filter);
	}
	
	private ServerWebExchange addClientHeaders(ServerWebExchange sanitizedExchange, ClientContext clientContext) {
		ServerHttpRequest request = sanitizedExchange.getRequest().mutate()
				.headers(headers -> {
					headers.set(USER_ID_HEADER, clientContext.userId());
					
					headers.set(USER_ROLE_HEADER, clientContext.role());
				})
				.build();
		
		return sanitizedExchange.mutate()
				.request(request)
				.build();
	}

	private ServerWebExchange removeClientHeaders(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest()
				.mutate()
				.headers(headers -> {
					headers.remove(USER_ID_HEADER);
					headers.remove(USER_ROLE_HEADER);
				})
				.build();
		
		return exchange.mutate()
				.request(request)
				.build();
	}

}
