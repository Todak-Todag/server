package com.spring.careplanservice.careplan.infrastructure.client;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeInternalResponse(
        boolean success,
        int code,
        String message,
        Data data
) {

    public record Data(
            UUID dischargeId,
            UUID patientId,
            LocalDate actualDate
    ) {
    }
}