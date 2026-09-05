package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeUpdateResult(
        UUID dischargeId,
        LocalDate scheduledDate
) {

    public static DischargeUpdateResult from(Discharge discharge) {
        return new DischargeUpdateResult(
                discharge.getId(),
                discharge.getScheduledDate()
        );
    }
}