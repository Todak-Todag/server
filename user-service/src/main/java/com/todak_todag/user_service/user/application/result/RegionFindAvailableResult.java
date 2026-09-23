package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

public record RegionFindAvailableResult(
        UUID regionId,
        String province,
        String district,
        String regionCode
) {

    // Entity → Result 변환
    public static RegionFindAvailableResult from(Region region) {
        return new RegionFindAvailableResult(
                region.getId(),
                region.getProvince(),
                region.getDistrict(),
                region.getRegionCode()
        );
    }
}