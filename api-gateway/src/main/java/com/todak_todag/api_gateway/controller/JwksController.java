package com.todak_todag.api_gateway.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.api_gateway.token.InternalKeyProvider;

import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.RsaPublicJwk;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class JwksController {

	private final InternalKeyProvider keyProvider;
	
	@GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> jwks() {
		RsaPublicJwk jwk = Jwks.builder()
				.key(keyProvider.getPublicKey())
				.id(keyProvider.getKeyId())
				.algorithm("RS256")
				.publicKeyUse("sig")
				.build();
		
		Map<String, Object> body = Jwks.set().add(jwk).build();
		
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
				.body(body);
	}
}
