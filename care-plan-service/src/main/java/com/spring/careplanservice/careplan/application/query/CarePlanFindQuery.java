package com.spring.careplanservice.careplan.application.query;

import java.util.UUID;

public record CarePlanFindQuery(
        UUID carePlanId,
        UUID userId
) {
}