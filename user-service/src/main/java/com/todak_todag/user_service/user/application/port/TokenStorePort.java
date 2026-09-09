package com.todak_todag.user_service.user.application.port;

import java.time.Duration;

public interface TokenStorePort {

	void storeAccessToken(String accessToken, String jwtAccessToken);
	
	void deleteAccessToken(String accessToken);
	
	void storeAccessTokenTemp(String accessToken, String jwtAccessToken, Duration ttl);
}
