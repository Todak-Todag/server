package com.todak_todag.user_service.user.application.port;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public interface TokenPort {
	String createPhantomToken();
	
	String hashPhantomToken(String phantomToken);
	
	String createAccessToken(UUID userId, UserRole role);
}
