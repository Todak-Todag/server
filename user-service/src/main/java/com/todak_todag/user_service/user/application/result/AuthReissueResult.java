package com.todak_todag.user_service.user.application.result;

public record AuthReissueResult(
		String newAccessToken,
		String newRefershToken
) {

}
