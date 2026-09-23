package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;

import java.util.UUID;

public record RegionFindAvailableResponse(
        UUID regionId,
        String province,
        String district,
        String regionCode
) {

    // Result → Response 변환
    public static RegionFindAvailableResponse from(RegionFindAvailableResult result) {
        return new RegionFindAvailableResponse(
                result.regionId(),
                result.province(),
                result.district(),
                result.regionCode()
        );
    }
}