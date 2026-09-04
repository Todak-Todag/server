package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.UUID;

//
public record RegionUpdateActiveResult(
        UUID regionId,
        boolean isActive
) {

    public static RegionUpdateActiveResult from(Region region) {
        return new RegionUpdateActiveResult(
                region.getId(),
                region.isActive()
        );
    }
}