package com.todak_todag.discharge_service.discharge.presentation.response;

import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DischargeFindResponse(
        UUID dischargeId,
        UUID patientId,
        UUID hospitalStaffId,
        String hospitalName,
        DischargeStatus status,
        LocalDate scheduledDate,
        LocalDate actualDate,
        Instant createdAt
) {

    public static DischargeFindResponse from(DischargeFindResult result) {
        return new DischargeFindResponse(
                result.dischargeId(),
                result.patientId(),
                result.hospitalStaffId(),
                result.hospitalName(),
                result.status(),
                result.scheduledDate(),
                result.actualDate(),
                result.createdAt()
        );
    }
}