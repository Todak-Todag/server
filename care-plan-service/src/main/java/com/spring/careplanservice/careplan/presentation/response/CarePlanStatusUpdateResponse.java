package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.global.common.UserRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CarePlanStatusUpdateResponse(
        @NotNull(message = "변경할 Care Plan 상태는 필수입니다.")
        CarePlanStatus status

) {

    public CarePlanStatusUpdateCommand toCommand(
            UUID userId,
            UserRole userRole,
            UUID carePlanId
    ) {
        return new CarePlanStatusUpdateCommand(
                userId,
                userRole,
                carePlanId,
                status
        );
    }
}