package com.spring.careplanservice.careplan.application.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.global.common.UserRole;

import java.util.UUID;

public record CarePlanStatusUpdateCommand(
        UUID userId,
        UserRole userRole,
        UUID carePlanId,
        CarePlanStatus status
) {
}