package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePreferenceSearchResult(
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate preferredDate,
        PreferredTimeSlot preferredTimeSlot,
        Instant createdAt
) {
    public static ServicePreferenceSearchResult from(
            ServicePreferenceView servicePreferenceView

    ) {
        return new ServicePreferenceSearchResult(
                servicePreferenceView.servicePreferenceId(),
                servicePreferenceView.provideServiceId(),
                servicePreferenceView.preferredDate(),
                servicePreferenceView.preferredTimeSlot(),
                servicePreferenceView.createdAt()
        );
    }
}
