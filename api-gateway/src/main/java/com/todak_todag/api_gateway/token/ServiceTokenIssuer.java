package com.todak_todag.api_gateway.token;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.todak_todag.api_gateway.config.InternalJwtProperties;

import io.jsonwebtoken.Jwts;

@Component
public class ServiceTokenIssuer {

	public static final String CLAIM_TOKEN_USE = "token_use";
	public static final String TOKEN_USE_S2S = "s2s";
	
	private final InternalKeyProvider keyProvider;
	
	private final InternalJwtProperties properties;
	
	private final Duration s2sTtl;
	
	public ServiceTokenIssuer(
			InternalKeyProvider keyProvider,
			InternalJwtProperties properteis,
			@Value("${internal-jwt.s2s-ttl:120s}") Duration s2sTtl
	) {
		this.keyProvider = keyProvider;
		this.properties = properteis;
		this.s2sTtl = s2sTtl;
	}
	
	public IssuedToken issue(String callServiceId, String audience) {
		Instant now = Instant.now();
		Instant exp = now.plus(s2sTtl);
		
		String token = Jwts.builder()
				.header().keyId(keyProvider.getKeyId()).and()
				.issuer(properties.issuer())
				.subject(callServiceId)
				.audience().add(audience).and()
				.claim(CLAIM_TOKEN_USE, TOKEN_USE_S2S)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.id(UUID.randomUUID().toString())
				.signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256)
				.compact();
		
		return new IssuedToken(token, s2sTtl.toSeconds());
	}
	
}
