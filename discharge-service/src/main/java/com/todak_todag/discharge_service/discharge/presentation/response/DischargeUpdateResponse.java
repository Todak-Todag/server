package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeUpdateResponse(
        UUID dischargeId,
        LocalDate scheduledDate
) {

    public static DischargeUpdateResponse from(
            DischargeUpdateResult result
    ) {
        return new DischargeUpdateResponse(
                result.dischargeId(),
                result.scheduledDate()
        );
    }
}