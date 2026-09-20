package com.todak_todag.user_service.global.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ServiceTokenClient {

	private final RestClient restClient;
	
	private final String callerId;
	
	private final String bootstrapKey;
	
	private final Map<String, Cached> cache = new ConcurrentHashMap<>();
	
	private record Cached(String token, Instant expiresAt) {
		boolean isValid() {
			return Instant.now().isBefore(expiresAt);
		}
	}
	
	public record TokenRequest(String caller, String audience) {}
	
	public record TokenResponse(String token, long expiresInSeconds) {}
	
	public ServiceTokenClient(
			@Value("${internal-jwt.token-endpoint}") String tokenEndpoint,
			@Value("${spring.application.name}") String callerId,
			@Value("${internal.key}") String bootstrapKey
	) {
		this.restClient = RestClient.create(tokenEndpoint);
    this.callerId = callerId;
    this.bootstrapKey = bootstrapKey;
	}
	
	public String getToken(String audience) {
		Cached c = cache.get(audience);
		if(c != null && c.isValid()) {
			return c.token();
		}
		
		TokenResponse response = restClient.post()
				.header("X-Internal-Api-Key", bootstrapKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new TokenRequest(callerId, audience))
				.retrieve()
				.body(TokenResponse.class);
		
		Instant expiresAt = Instant.now().plusSeconds(response.expiresInSeconds() - 10);
		cache.put(audience, new Cached(response.token(), expiresAt));
		return response.token();
	}
}
