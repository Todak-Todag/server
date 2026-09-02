package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeInternalFindResult(
        UUID dischargeId,
        UUID patientId,
        LocalDate actualDate
) {

    public static DischargeInternalFindResult from(
            Discharge discharge
    ) {
        return new DischargeInternalFindResult(
                discharge.getId(),
                discharge.getPatientId(),
                discharge.getActualDate()
        );
    }
}