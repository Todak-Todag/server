package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.RegionUpdateActiveCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegionUpdateActiveRequest(
        @NotNull(message = "활성화 여부는 필수입니다.")
        Boolean isActive
) {

    public RegionUpdateActiveCommand toCommand(UUID regionId) {
        return new RegionUpdateActiveCommand(
                regionId,
                isActive
        );
    }
}