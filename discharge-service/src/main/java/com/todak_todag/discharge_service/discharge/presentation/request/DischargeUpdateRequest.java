package com.todak_todag.discharge_service.discharge.presentation.request;

import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DischargeUpdateRequest(
        DischargeStatus status,
        LocalDate scheduledDate

) {
}