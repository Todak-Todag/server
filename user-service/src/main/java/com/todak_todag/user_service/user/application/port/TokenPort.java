package com.todak_todag.user_service.user.application.port;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public interface TokenPort {
	String createToken();
	
	String hashAccessToken(String accessToken);
	
	String createJwtAccessToken(UUID userId, UserRole role);
}
