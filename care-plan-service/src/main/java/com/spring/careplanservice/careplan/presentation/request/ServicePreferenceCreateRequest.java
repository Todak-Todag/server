package com.spring.careplanservice.careplan.presentation.request;

import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceCreateRequest(
        @NotNull(message = "희망 날짜는 필수입니다.")
        LocalDate preferredDate,

        @NotNull(message = "희망 시간대는 필수입니다.")
        PreferredTimeSlot preferredTimeSlot

) {

    public ServicePreferenceCreateCommand toCommand(
            UUID userId,
            UUID planServiceId
    ) {
        return new ServicePreferenceCreateCommand(
                userId,
                planServiceId,
                preferredDate,
                preferredTimeSlot
        );
    }
}