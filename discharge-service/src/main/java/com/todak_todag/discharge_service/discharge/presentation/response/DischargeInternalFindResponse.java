package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeInternalFindResult;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeInternalFindResponse(
        UUID dischargeId,
        UUID patientId,
        LocalDate actualDate
) {

    public static DischargeInternalFindResponse from(
            DischargeInternalFindResult result
    ) {
        return new DischargeInternalFindResponse(
                result.dischargeId(),
                result.patientId(),
                result.actualDate()
        );
    }
}