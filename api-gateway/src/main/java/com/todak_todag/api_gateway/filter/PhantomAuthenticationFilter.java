package com.todak_todag.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.authentication.ClientContext;
import com.todak_todag.api_gateway.token.InternalTokenIssuer;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PhantomAuthenticationFilter implements GlobalFilter, Ordered {

  private static final String USER_ID_HEADER = "X-User-Id";
	
	private static final String USER_ROLE_HEADER = "X-User-Role";
	
	private static final String GATEWAY_TOKEN_HEADER = "X-Gateway-Token";
	
	private final InternalTokenIssuer tokenIssuer;
	
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
		String audience = resolveAudience(sanitizedExchange);
		
		String gatewayToken = tokenIssuer.issue(clientContext, audience);
		
		ServerHttpRequest request = sanitizedExchange.getRequest().mutate()
				.headers(headers -> {
					headers.set(USER_ID_HEADER, clientContext.userId());
					headers.set(USER_ROLE_HEADER, clientContext.role());
					headers.set(GATEWAY_TOKEN_HEADER, gatewayToken);
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
					headers.remove(GATEWAY_TOKEN_HEADER);
				})
				.build();
		
		return exchange.mutate()
				.request(request)
				.build();
	}
	
	private String resolveAudience(ServerWebExchange exchange) {
		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
		
		return (route != null) ? route.getId() : "unknown";
	}

}
