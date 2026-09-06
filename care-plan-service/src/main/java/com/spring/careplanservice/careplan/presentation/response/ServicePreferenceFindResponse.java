package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.ServicePreferenceFindResult;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceFindResponse(
        UUID servicePreferenceId,
        UUID planServiceId,
        UUID provideServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot,
        Instant createdAt
) {

    public static ServicePreferenceFindResponse from(
            ServicePreferenceFindResult servicePreferenceFindResult
    ) {
        return new ServicePreferenceFindResponse(
                servicePreferenceFindResult.servicePreferenceId(),
                servicePreferenceFindResult.planServiceId(),
                servicePreferenceFindResult.provideServiceId(),
                servicePreferenceFindResult.preferredDate(),
                servicePreferenceFindResult.preferredTimeSlot(),
                servicePreferenceFindResult.createdAt()
        );
    }
}
