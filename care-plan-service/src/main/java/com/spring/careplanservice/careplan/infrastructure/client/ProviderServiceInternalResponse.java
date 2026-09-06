package com.spring.careplanservice.careplan.infrastructure.client;

import java.util.List;
import java.util.UUID;

public record ProviderServiceInternalResponse(
        boolean success,
        int code,
        String message,
        Data data
) {

    public record Data(
            List<Content> content
    ) {
    }

    public record Content(
            UUID provideServiceId,
            String name,
            String content
    ) {
    }
}
