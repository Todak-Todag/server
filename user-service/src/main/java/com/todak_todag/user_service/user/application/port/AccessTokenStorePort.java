package com.todak_todag.user_service.user.application.port;

public interface AccessTokenStorePort {

	void storeAccessToken(String accessToken, String jwtAccessToken);
}
