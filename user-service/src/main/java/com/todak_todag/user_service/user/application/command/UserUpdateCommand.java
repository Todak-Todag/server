package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

import com.todak_todag.user_service.global.security.UserContext;

public record UserUpdateCommand(
		String name,
		String phone,
		UUID regionId,
		String address,
		UserContext user
) {

}
