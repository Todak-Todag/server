package com.spring.careplanservice.careplan.application.query;

import com.spring.careplanservice.global.common.UserRole;

import java.util.UUID;

public record ServicePreferenceFindQuery(
        UUID userId,
        UserRole role,
        UUID servicePreferenceId
) {
}
