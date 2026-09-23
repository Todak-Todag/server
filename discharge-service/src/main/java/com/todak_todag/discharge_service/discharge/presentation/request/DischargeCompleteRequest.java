package com.todak_todag.discharge_service.discharge.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DischargeCompleteRequest(

        @NotNull(message = "실제 퇴원일은 필수입니다.")
        @PastOrPresent(message = "실제 퇴원일은 미래일 수 없습니다.")
        LocalDate actualDate

) {
}