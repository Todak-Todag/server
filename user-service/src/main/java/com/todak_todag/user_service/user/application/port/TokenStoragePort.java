package com.todak_todag.user_service.user.application.port;

import java.util.Optional;

import com.todak_todag.user_service.user.domain.entity.auth.PhantomToken;

public interface TokenStoragePort {

	void saveToken(PhantomToken phantomToken);
	
	Optional<PhantomToken> findAccessTokenByHash(String phantomToken);
	
	void deleteToken(String phantomToken);
}
