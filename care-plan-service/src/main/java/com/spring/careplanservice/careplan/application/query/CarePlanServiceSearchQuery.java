package com.spring.careplanservice.careplan.application.query;

import java.util.UUID;

public record CarePlanServiceSearchQuery(
        UUID userId,
        UUID carePlanId,
        Integer page,
        Integer size
) {
}
