package com.todak_todag.discharge_service.discharge.application.command;

import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeUpdateCommand(
        UUID dischargeId,
        UUID hospitalStaffId,
        DischargeStatus status,
        LocalDate scheduledDate
) {
}