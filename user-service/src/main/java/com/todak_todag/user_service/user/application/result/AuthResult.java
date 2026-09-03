package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public final class AuthResult {

	public record AuthLoginResult(
			UUID userId,
			String accessToken,
			String refreshToken,
			String jwtAccessToken
	) {}
}
