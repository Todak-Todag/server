package com.todak_todag.discharge_service.discharge.application.result;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DischargeFindResult(
        UUID dischargeId,
        UUID patientId,
        UUID hospitalStaffId,
        String hospitalName,
        DischargeStatus status,
        LocalDate scheduledDate,
        LocalDate actualDate,
        Instant createdAt
) {

    public static DischargeFindResult from(Discharge discharge) {
        return new DischargeFindResult(
                discharge.getId(),
                discharge.getPatientId(),
                discharge.getHospitalStaffId(),
                discharge.getHospitalName(),
                discharge.getStatus(),
                discharge.getScheduledDate(),
                discharge.getActualDate(),
                discharge.getCreatedAt()
        );
    }
}