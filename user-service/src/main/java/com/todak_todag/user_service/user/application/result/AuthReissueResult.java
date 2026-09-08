package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public record AuthReissueResult(
		UUID userId,
		String newAccessToken,
		String newRefershToken
) {

}
