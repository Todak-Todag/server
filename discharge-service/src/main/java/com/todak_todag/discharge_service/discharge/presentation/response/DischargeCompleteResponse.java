package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeCompleteResult;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeCompleteResponse(
        UUID dischargeId,
        DischargeStatus status,
        LocalDate actualDate
) {

    public static DischargeCompleteResponse from(
            DischargeCompleteResult result
    ) {
        return new DischargeCompleteResponse(
                result.dischargeId(),
                result.status(),
                result.actualDate()
        );
    }
}