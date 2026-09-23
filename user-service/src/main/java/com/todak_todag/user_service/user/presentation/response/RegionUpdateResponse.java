package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionUpdateResult;

import java.util.UUID;

public record RegionUpdateResponse(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    public static RegionUpdateResponse from(RegionUpdateResult result) {
        return new RegionUpdateResponse(
                result.regionId(),
                result.province(),
                result.district(),
                result.regionCode(),
                result.isActive()
        );
    }
}