package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

import java.util.UUID;

public record ServicePreferenceUpdateResult(
        UUID servicePreferenceId
) {

    public static ServicePreferenceUpdateResult from(
            CarePlanServicePreference preference
    ) {
        return new ServicePreferenceUpdateResult(
                preference.getId()
        );
    }
}
