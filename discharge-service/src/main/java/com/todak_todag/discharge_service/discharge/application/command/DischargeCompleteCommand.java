package com.todak_todag.discharge_service.discharge.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeCompleteCommand(
        UUID dischargeId,
        UUID hospitalStaffId,
        LocalDate actualDate
) {
}