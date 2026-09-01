package com.todak_todag.api_gateway.token;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.todak_todag.api_gateway.filter.ClientContext;

public final class ClientAuthenticationToken extends AbstractAuthenticationToken {

	private final ClientContext client;
	
	private String credentials;
	
	private ClientAuthenticationToken(String accessToken) {
		super(List.of());
		
		if(accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("");
		}
		
		this.client = null;
		this.credentials = accessToken;
		
		super.setAuthenticated(false);
	}
	
	private ClientAuthenticationToken(ClientContext client, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		
		if(client == null) {
			throw new IllegalArgumentException("");
		}
		
		this.client = client;
		this.credentials = null;
		
		super.setAuthenticated(true);
	}
	
	public static ClientAuthenticationToken authenticated(
			ClientContext client,
			Collection<? extends GrantedAuthority> authorities
	) {
		return new ClientAuthenticationToken(client, authorities);
	}

	@Override
	public Object getCredentials() {
		return this.credentials;
	}

	@Override
	public ClientContext getPrincipal() {
		return this.client;
	}
	
	@Override
	public void eraseCredentials() {
		super.eraseCredentials();
		credentials = null;
	}
	
	@Override
	public void setAuthenticated(boolean authenticated) {
		if(authenticated) {
			throw new IllegalArgumentException("");
		}
		
		super.setAuthenticated(false);
	}
	
}
