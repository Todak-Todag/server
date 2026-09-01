package com.todak_todag.discharge_service.discharge.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeCreateCommand(
        UUID hospitalStaffId,
        UUID patientId,
        String hospitalName,
        LocalDate scheduledDate
) {
}