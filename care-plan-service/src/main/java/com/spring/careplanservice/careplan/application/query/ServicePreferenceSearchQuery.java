package com.spring.careplanservice.careplan.application.query;

import com.spring.careplanservice.global.common.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceSearchQuery(
        UUID userId,
        UserRole role,
        UUID carePlanId,
        LocalDate preferredDate,
        Integer page,
        Integer size
) {
}
