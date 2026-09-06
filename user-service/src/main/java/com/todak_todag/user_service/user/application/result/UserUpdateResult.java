package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public record UserUpdateResult(
		UUID userId,
		String name,
		String phone,
		UUID regionId,
		String address
) {

}
