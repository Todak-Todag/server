package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public record UserSignupCreatedResult(UUID userId, String name) {}
