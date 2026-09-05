package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceSearchResponse(
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot,
        Instant createdAt
) {

    public static ServicePreferenceSearchResponse from(
            ServicePreferenceSearchResult servicePreferenceSearchResult
    ) {
        return new ServicePreferenceSearchResponse(
                servicePreferenceSearchResult.servicePreferenceId(),
                servicePreferenceSearchResult.provideServiceId(),
                servicePreferenceSearchResult.preferredDate(),
                servicePreferenceSearchResult.preferredTimeSlot(),
                servicePreferenceSearchResult.createdAt()
        );
    }
}
