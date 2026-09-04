package com.spring.careplanservice.careplan.application.event;

import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;

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
            PreferredTimeSlot preferredTimeSlot
    ) {
    }
}
