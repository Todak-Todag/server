package com.spring.careplanservice.careplan.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CarePlanServiceSelectRequest(
        @NotNull(message = "서비스 종류 ID는 필수입니다.")
        UUID provideServiceId
) {
}
