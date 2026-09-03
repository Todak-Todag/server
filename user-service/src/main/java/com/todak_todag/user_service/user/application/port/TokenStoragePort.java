package com.todak_todag.user_service.user.application.port;

import java.util.Optional;

import com.todak_todag.user_service.user.domain.entity.auth.AccessToken;

public interface TokenStoragePort {

	void saveToken(AccessToken phantomToken);
	
	Optional<AccessToken> findAccessTokenByHash(String phantomToken);
	
	void deleteToken(String phantomToken);
}
