package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

import java.util.UUID;

public record DischargeUpdateResult(
        UUID dischargeId
) {

    public static DischargeUpdateResult from(
            Discharge discharge
    ) {
        return new DischargeUpdateResult(
                discharge.getId()
        );
    }
}