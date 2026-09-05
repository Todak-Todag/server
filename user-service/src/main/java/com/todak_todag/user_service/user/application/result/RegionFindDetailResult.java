package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

//지역 단건 조회 결과
public record RegionFindDetailResult(
        UUID regionId,
        String province,
        String district,
        String regionCode,
        boolean isActive
) {

    public static RegionFindDetailResult from(Region region) {
        return new RegionFindDetailResult(
                region.getId(),
                region.getProvince(),
                region.getDistrict(),
                region.getRegionCode(),
                region.isActive()
        );
    }
}