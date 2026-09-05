package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

// 지역 등록 후 응답 양식
public record RegionCreateResult(
        UUID regionId
) {

    public static RegionCreateResult from(Region region) {
        return new RegionCreateResult(
                region.getId()
        );
    }
}