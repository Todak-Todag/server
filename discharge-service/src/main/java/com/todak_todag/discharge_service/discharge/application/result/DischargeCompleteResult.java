package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeCompleteResult(
        UUID dischargeId,
        DischargeStatus status,
        LocalDate actualDate
) {

    public static DischargeCompleteResult from(
            Discharge discharge
    ) {
        return new DischargeCompleteResult(
                discharge.getId(),
                discharge.getStatus(),
                discharge.getActualDate()
        );
    }
}