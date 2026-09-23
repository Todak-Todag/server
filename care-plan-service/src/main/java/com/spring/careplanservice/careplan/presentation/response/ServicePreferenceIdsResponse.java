package com.spring.careplanservice.careplan.presentation.response;

import com.spring.careplanservice.careplan.application.result.ServicePreferenceIdsResult;

import java.util.List;
import java.util.UUID;

public record ServicePreferenceIdsResponse(
        List<UUID> content
) {

    public static ServicePreferenceIdsResponse from(
            ServicePreferenceIdsResult servicePreferenceIdsResult
    ) {
        return new ServicePreferenceIdsResponse(
                servicePreferenceIdsResult.servicePreferenceIds()
        );
    }
}