package com.spring.careplanservice.careplan.application.result;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;

import java.util.UUID;

public record ServicePreferenceCreateResult(
        UUID servicePreferenceId
) {

    public static ServicePreferenceCreateResult from(
            CarePlanServicePreference preference
    ) {
        return new ServicePreferenceCreateResult(
                preference.getId()
        );
    }
}
