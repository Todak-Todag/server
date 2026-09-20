package com.todak_todag.api_gateway.controller;

import static org.mockito.ArgumentMatchers.matches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.api_gateway.token.IssuedToken;
import com.todak_todag.api_gateway.token.ServiceTokenIssuer;

import reactor.core.publisher.Mono;

@RestController
public class InternalTokenController {

	private final ServiceTokenIssuer issuer;
	
	private final String bootstrapKey;
	
	public InternalTokenController(
			ServiceTokenIssuer issuer,
			@Value("${internal.key}") String bootstrapKey
	) {
		this.issuer = issuer;
		this.bootstrapKey = bootstrapKey;
	}
	
	@PostMapping("/internal/s2s-token")
	public Mono<ResponseEntity<TokenResponse>> issue(
			@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
			@RequestBody TokenRequest request
	) {
		if(!matches(apiKey)) {
			return Mono.just(ResponseEntity.status(401).build());
		}
		IssuedToken issued = issuer.issue(request.caller(), request.audience());
		
		return Mono.just(ResponseEntity.ok(
				new TokenResponse(issued.token(), issued.expiresInSeconds())
		));
	}

	private boolean matches(String given) {
		if(given == null || given.isBlank()) return false;
		
		return MessageDigest.isEqual(
				bootstrapKey.getBytes(StandardCharsets.UTF_8),
				given.getBytes(StandardCharsets.UTF_8)
		);
	}
}
