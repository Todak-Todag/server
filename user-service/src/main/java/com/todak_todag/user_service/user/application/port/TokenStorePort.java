package com.todak_todag.user_service.user.application.port;

import java.time.Duration;
import java.util.UUID;

public interface TokenStorePort {

	void storeAccessToken(UUID userId, String accessToken, String jwtAccessToken);
	
	void deleteAccessToken(UUID userId, String accessToken);
	
	void storeAccessTokenTemp(UUID userId, String accessToken, String jwtAccessToken, Duration ttl);
	
	void revokeAllSessions(UUID userId);
}
