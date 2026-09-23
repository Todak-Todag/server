package com.spring.careplanservice.careplan.presentation.request;

import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CarePlanServiceSelectRequest(
        @NotNull(message = "서비스 종류 ID는 필수입니다.")
        UUID provideServiceId
) {
    public CarePlanServiceSelectCommand toCommand(
            UUID userId,
            UUID carePlanId
    ) {
        return new CarePlanServiceSelectCommand(
                userId,
                carePlanId,
                provideServiceId
        );
    }
}
