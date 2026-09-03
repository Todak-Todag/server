package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public final class UserResult {

	public record UserSignupCreatedResult(UUID userId, String name) {}
	
	public record UserAdminCreatedResult() {}
}
