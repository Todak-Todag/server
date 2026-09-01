package com.spring.careplanservice.global.security;

import com.spring.careplanservice.global.common.UserRole;

import java.util.UUID;

public record UserContext(
        UUID userId,
        UserRole role
) {
}
