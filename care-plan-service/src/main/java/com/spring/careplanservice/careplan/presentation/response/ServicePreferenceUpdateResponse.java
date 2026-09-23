package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.ServicePreferenceUpdateResult;

import java.util.UUID;

public record ServicePreferenceUpdateResponse(
        UUID servicePreferenceId
) {

    public static ServicePreferenceUpdateResponse from(
            ServicePreferenceUpdateResult servicePreferenceUpdateResult
    ) {
        return new ServicePreferenceUpdateResponse(
                servicePreferenceUpdateResult.servicePreferenceId()
        );
    }
}
