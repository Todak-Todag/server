package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

public record UserUpdateResponse(
		UUID userId,
		String name,
		String phone,
		UUID regionId,
		String address
) {

}
