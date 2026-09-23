package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

public record UserAdminCreateCommand(
		String username,
		String password,
		String name,
		String phone,
		UUID regionId
) {

}
