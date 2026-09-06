package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

// 수정 이후 반환 목록
public record RegionUpdateResult(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    public static RegionUpdateResult from(Region region) {
        return new RegionUpdateResult(
                region.getId(),
                region.getProvince(),
                region.getDistrict(),
                region.getRegionCode(),
                region.isActive()
        );
    }
}