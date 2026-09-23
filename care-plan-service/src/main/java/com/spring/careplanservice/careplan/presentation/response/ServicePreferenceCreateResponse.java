package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;

import java.util.UUID;

public record ServicePreferenceCreateResponse(
        UUID servicePreferenceId
) {

    public static ServicePreferenceCreateResponse from(
            ServicePreferenceCreateResult servicePreferenceCreateResult
    ) {
        return new ServicePreferenceCreateResponse(
                servicePreferenceCreateResult.servicePreferenceId()
        );
    }
}