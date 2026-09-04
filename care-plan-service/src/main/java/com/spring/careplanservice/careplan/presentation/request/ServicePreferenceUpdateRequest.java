package com.spring.careplanservice.careplan.presentation.request;

import com.spring.careplanservice.careplan.application.command.ServicePreferenceUpdateCommand;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceUpdateRequest(
        @NotNull(message = "희망 날짜는 필수입니다.")
        LocalDate preferredDate,

        @NotNull(message = "희망 시간대는 필수입니다.")
        PreferredTimeSlot preferredTimeSlot

) {

    public ServicePreferenceUpdateCommand toCommand(
            UUID userId,
            UUID servicePreferenceId
    ) {
        return new ServicePreferenceUpdateCommand(
                userId,
                servicePreferenceId,
                preferredDate,
                preferredTimeSlot
        );
    }
}
