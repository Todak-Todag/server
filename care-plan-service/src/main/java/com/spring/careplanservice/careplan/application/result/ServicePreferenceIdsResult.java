package com.spring.careplanservice.careplan.application.result;

import java.util.List;
import java.util.UUID;

public record ServicePreferenceIdsResult(
        List<UUID> servicePreferenceIds
) {
}