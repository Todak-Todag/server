package com.todak_todag.discharge_service.discharge.presentation.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record DischargeCreateRequest(

        @NotNull
        UUID patientId,

        @NotBlank
        String hospitalName,

        @NotNull
        @Future
        LocalDate scheduledDate

) {
}