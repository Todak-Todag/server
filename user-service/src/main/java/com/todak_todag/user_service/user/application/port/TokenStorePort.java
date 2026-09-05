package com.todak_todag.user_service.user.application.port;

public interface TokenStorePort {

	void storeAccessToken(String accessToken, String jwtAccessToken);
}
