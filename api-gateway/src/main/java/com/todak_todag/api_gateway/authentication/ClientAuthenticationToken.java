package com.todak_todag.api_gateway.authentication;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class ClientAuthenticationToken extends AbstractAuthenticationToken {

	private final ClientContext client;
	
	private String credentials;
	
	private ClientAuthenticationToken(String accessToken) {
		super(List.of());
		
		if(accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("인증 시도 객체를 만들려면 액세스 토큰이 필요합니다.");
		}
		
		this.client = null;
		this.credentials = accessToken;
		
		super.setAuthenticated(false);
	}
	
	private ClientAuthenticationToken(ClientContext client, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		
		if(client == null) {
			throw new IllegalArgumentException("인증 완료 객체를 만들려면 검증된 클라이언트 정보가 필요합니다.");
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
	
	public static ClientAuthenticationToken unauthenticated(
			String accessToken
	) {
		return new ClientAuthenticationToken(accessToken);
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
			throw new IllegalArgumentException("인증 완료 상태는 직접 지정할 수 없습니다.");
		}
		
		super.setAuthenticated(false);
	}
	
}
