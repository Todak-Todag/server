package com.todak_todag.api_gateway.authentication;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;
import com.todak_todag.api_gateway.store.AccessTokenStore;
import com.todak_todag.api_gateway.token.AccessTokenClaims;
import com.todak_todag.api_gateway.token.JwtTokenParser;
import com.todak_todag.api_gateway.token.TokenHashGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ClientAuthenticationManager implements ReactiveAuthenticationManager {

	private final TokenHashGenerator tokenHashGenerator;
  private final AccessTokenStore accessTokenStore;
  private final JwtTokenParser jwtTokenParser;
  
	@Override
	public Mono<Authentication> authenticate(Authentication authentication) {
		if(!(authentication instanceof ClientAuthenticationToken clientToken)) {
			return Mono.empty();
		}
		
		Object credentials = clientToken.getCredentials();
		
		if(!(credentials instanceof String cookieToken) || cookieToken.isBlank()) {
			return Mono.error(new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN));
		}
		
		String tokenHash = tokenHashGenerator.generate(cookieToken);
		
		return accessTokenStore.findByHash(tokenHash)
        .onErrorMap(
        		DataAccessException.class,
            exception -> new TokenException(TokenErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE, exception)
        )
        .switchIfEmpty(
        		Mono.error(new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN))
        )
        .map(jwtTokenParser::parse)
        .map(this::createAuthenticatedToken)
        .doFinally(signalType -> clientToken.eraseCredentials())
        ;
	}
  
	private Authentication createAuthenticatedToken(AccessTokenClaims claims) {
	  ClientContext clientContext = new ClientContext(
	  		claims.userId(),
	  		claims.role()
	  );
	
	  SimpleGrantedAuthority authority = 
	  		new SimpleGrantedAuthority("ROLE_" + claims.role());
	
	  return ClientAuthenticationToken.authenticated(
	  		clientContext,
	  		List.of(authority)
	  );
	}
  
}
