package com.spring.careplanservice.careplan.presentation.request;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CarePlanCreateRequest(
        @NotNull(message = "환자 ID는 필수입니다.")
        UUID patientId,

        @NotNull(message = "퇴원 ID는 필수입니다.")
        UUID dischargeId,

        String note,

        List<UUID> provideServiceIds
) {

    public CarePlanCreateCommand toCommand(UUID userId) {
        return new CarePlanCreateCommand(
                patientId,
                dischargeId,
                note,
                provideServiceIds,
                userId
        );
    }
}