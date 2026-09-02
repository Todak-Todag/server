package com.todak_todag.provider_service.provider.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServiceOfferingCreateRequest(
        @NotNull(message = "provideServiceId는 필수입니다.")
        UUID provideServiceId
) {
}