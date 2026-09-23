package com.spring.careplanservice.careplan.application.query;

import java.util.UUID;

public record CarePlanServiceFindQuery(
        UUID userId,
        UUID carePlanId,
        UUID planServiceId
) {
}
