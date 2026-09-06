package com.spring.careplanservice.careplan.infrastructure.client;

import java.util.UUID;

public record ScheduleInternalResponse(
        boolean success,
        int code,
        String message,
        Data data
) {

    public record Data(
            UUID serviceResultId,
            UUID serviceScheduleId
    ) {
    }
}