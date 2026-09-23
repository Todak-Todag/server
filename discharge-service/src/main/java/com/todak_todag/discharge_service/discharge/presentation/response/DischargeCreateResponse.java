package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;

import java.util.UUID;

public record DischargeCreateResponse(
        UUID dischargeId
) {

    public static DischargeCreateResponse from(
            DischargeCreateResult result
    ) {
        return new DischargeCreateResponse(
                result.dischargeId()
        );
    }
}