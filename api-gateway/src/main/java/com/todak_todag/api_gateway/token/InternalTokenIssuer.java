package com.todak_todag.api_gateway.token;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.todak_todag.api_gateway.authentication.ClientContext;
import com.todak_todag.api_gateway.config.InternalJwtProperties;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InternalTokenIssuer {

	private final InternalKeyProvider keyProvider;
	
	private final InternalJwtProperties properties;
	
	public String issue(ClientContext client, String audience) {
		Instant now = Instant.now();
		
		return Jwts.builder()
				.header().keyId(keyProvider.getKeyId()).and()
				.issuer(properties.issuer())
				.subject(client.userId())
				.audience().add(audience).and()
				.claim("role", client.role())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(properties.ttl())))
				.id(UUID.randomUUID().toString())
				.signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256)
				.compact();
	}
}
