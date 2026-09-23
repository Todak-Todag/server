package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.global.common.UserRole;

import java.util.UUID;

public record UserInternalReadResult(
        UUID userId,
        UserRole role,
        UUID regionId
) {
}
