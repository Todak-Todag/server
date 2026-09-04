package com.todak_todag.user_service.user.application.port;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public interface TokenPort {
	String createToken();
	
	String hashToken(String token);
	
	String createJwtAccessToken(UUID userId, UserRole role);
}
