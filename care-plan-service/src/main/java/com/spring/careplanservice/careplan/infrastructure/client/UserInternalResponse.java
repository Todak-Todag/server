package com.spring.careplanservice.careplan.infrastructure.client;

import com.spring.careplanservice.global.common.UserRole;

import java.util.UUID;

public record UserInternalResponse(
        boolean success,
        int code,
        String message,
        Data data
) {

    public record Data(
            UUID userId,
            UserRole role,
            UUID regionId
    ) {
    }
}