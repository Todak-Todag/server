package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.global.common.UserRole;

import java.util.UUID;

public record UserFindResult(
        UUID userId,
        UserRole role,
        UUID regionId
) {
}
