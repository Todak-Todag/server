package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;

import java.util.UUID;

public record DischargeUpdateResponse(
        UUID dischargeId
) {

    public static DischargeUpdateResponse from(
            DischargeUpdateResult result
    ) {
        return new DischargeUpdateResponse(
                result.dischargeId()
        );
    }
}