package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.global.common.UserRole;

import java.util.UUID;

public record UserInternalReadResponse(
        UUID userId,
        UserRole role,
        UUID regionId
) {}
