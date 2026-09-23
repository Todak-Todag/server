package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionFindAdminResult;

import java.util.UUID;

public record RegionFindAdminResponse(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    // Result → Response 변환
    public static RegionFindAdminResponse from(RegionFindAdminResult result) {
        return new RegionFindAdminResponse(
                result.regionId(),
                result.province(),
                result.district(),
                result.regionCode(),
                result.isActive()
        );
    }
}