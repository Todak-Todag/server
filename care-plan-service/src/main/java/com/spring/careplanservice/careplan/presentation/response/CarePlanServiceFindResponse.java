package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.CarePlanServiceFindResult;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CarePlanServiceFindResponse(
        UUID planServiceId,
        UUID provideServiceId,
        String provideServiceName,
        String provideServiceContent,
        List<PreferenceResponse> preferences,
        Instant createdAt
) {
    public static CarePlanServiceFindResponse from(
            CarePlanServiceFindResult carePlanServiceFindResult
    ) {
        return new CarePlanServiceFindResponse(
                carePlanServiceFindResult.planServiceId(),
                carePlanServiceFindResult.provideServiceId(),
                carePlanServiceFindResult.provideServiceName(),
                carePlanServiceFindResult.provideServiceContent(),
                carePlanServiceFindResult.preferences().stream()
                        .map(PreferenceResponse::from)
                        .toList(),
                carePlanServiceFindResult.createdAt()
        );
    }

    public record PreferenceResponse(
            UUID servicePreferenceId,
            LocalDate preferredDate,
            PreferredTimeSlot preferredTimeSlot
    ) {
        public static PreferenceResponse from(
                CarePlanServiceFindResult.PreferenceSummary preferenceSummary
        ) {
            return new PreferenceResponse(
                    preferenceSummary.servicePreferenceId(),
                    preferenceSummary.preferredDate(),
                    preferenceSummary.preferredTimeSlot()
            );
        }
    }
}
