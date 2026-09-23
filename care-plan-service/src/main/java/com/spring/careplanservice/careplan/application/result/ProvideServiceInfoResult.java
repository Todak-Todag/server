package com.spring.careplanservice.careplan.application.result;

import java.util.UUID;

public record ProvideServiceInfoResult(
        UUID provideServiceId,
        String name,
        String content
) {
}
