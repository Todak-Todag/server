package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeSearchResult(
        UUID dischargeId,
        UUID patientId,
        String hospitalName,
        DischargeStatus status,
        LocalDate scheduledDate,
        LocalDate actualDate
) {

    public static DischargeSearchResult from(Discharge discharge) {
        return new DischargeSearchResult(
                discharge.getId(),
                discharge.getPatientId(),
                discharge.getHospitalName(),
                discharge.getStatus(),
                discharge.getScheduledDate(),
                discharge.getActualDate()
        );
    }
}