package com.todak_todag.discharge_service.discharge.presentation.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DischargeUpdateRequest(

        @NotNull
        @Future
        LocalDate scheduledDate

) {
}