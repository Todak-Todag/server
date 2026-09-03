package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

public record RegionFindAdminResult(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    // Entity → Result 변환
    public static RegionFindAdminResult from(Region region) {
        return new RegionFindAdminResult(
                region.getId(),
                region.getProvince(),
                region.getDistrict(),
                region.getRegionCode(),
                region.isActive()
        );
    }
}