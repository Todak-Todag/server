package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionUpdateActiveResult;

import java.util.UUID;

public record RegionUpdateActiveResponse(
        UUID regionId,
        boolean isActive
) {

    public static RegionUpdateActiveResponse from(
            RegionUpdateActiveResult result
    ) {
        return new RegionUpdateActiveResponse(
                result.regionId(),
                result.isActive()
        );
    }
}