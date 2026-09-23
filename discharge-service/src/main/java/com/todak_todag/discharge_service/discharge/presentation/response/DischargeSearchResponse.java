package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeSearchResult;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeSearchResponse(
        UUID dischargeId,
        UUID patientId,
        String hospitalName,
        DischargeStatus status,
        LocalDate scheduledDate,
        LocalDate actualDate
) {

    public static DischargeSearchResponse from(
            DischargeSearchResult result
    ) {
        return new DischargeSearchResponse(
                result.dischargeId(),
                result.patientId(),
                result.hospitalName(),
                result.status(),
                result.scheduledDate(),
                result.actualDate()
        );
    }
}