package com.todak_todag.user_service.user.application.event;

public record LoginSuccessEvent(
		String accessToken,
		String jwtAccessToken
) {

}
