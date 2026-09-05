package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionFindDetailResult;

import java.util.UUID;

// 지역 단건 조회 응답 양식
public record RegionFindDetailResponse(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    public static RegionFindDetailResponse from(RegionFindDetailResult result) {
        return new RegionFindDetailResponse(
                result.regionId(),
                result.province(),
                result.district(),
                result.regionCode(),
                result.isActive()
        );
    }
}