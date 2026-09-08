package com.todak_todag.api_gateway.authentication;

import java.util.List;

import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.config.AuthenticationProperties;
import com.todak_todag.api_gateway.config.RefreshTokenProperties;
import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ClientCookieConverter implements ServerAuthenticationConverter {

	private final AuthenticationProperties authenticationProperties;
	
	private final RefreshTokenProperties refreshTokenProperties;

	@Override
	public Mono<Authentication> convert(ServerWebExchange exchange) {
		String cookieName = authenticationProperties.cookieName();
		
		List<HttpCookie> cookies = exchange.getRequest()
				.getCookies()
				.get(cookieName);
		
		if(cookies == null || cookies.isEmpty()) {
			return hasRefreshTokenCookie(exchange)
					? Mono.error(new TokenException(TokenErrorCode.EXPIRED_ACCESS_TOKEN))
					: Mono.empty();
		}
		
		if(cookies.size() != 1) {
			return Mono.error(new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN));
		}
		
		String accessToken = cookies.get(0).getValue();
		
		if(accessToken == null || accessToken.isBlank()) {
			return Mono.error(new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN));
		}
		
		Authentication authentication = ClientAuthenticationToken.unauthenticated(accessToken);
		
		return Mono.just(authentication);
	}

	private boolean hasRefreshTokenCookie(ServerWebExchange exchange) {
		List<HttpCookie> refreshCookies = exchange.getRequest()
				.getCookies()
				.get(refreshTokenProperties.cookieName());
		
		return refreshCookies != null
				&& refreshCookies.stream().anyMatch(cookie -> !cookie.getValue().isBlank());
	}
	
	
}
