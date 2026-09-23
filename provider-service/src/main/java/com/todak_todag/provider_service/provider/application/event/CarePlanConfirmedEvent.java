package com.todak_todag.provider_service.provider.application.event;

import com.todak_todag.provider_service.global.common.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CarePlanConfirmedEvent(
        UUID carePlanId,
        UUID regionId,
        List<Service> services
) {

    public record Service(
            UUID planServiceId,
            UUID provideServiceId,
            List<Preference> preferences
    ) {
    }

    public record Preference(
            UUID servicePreferenceId,
            LocalDate preferredDate,
            TimeSlot preferredTimeSlot
    ) {
    }
}
