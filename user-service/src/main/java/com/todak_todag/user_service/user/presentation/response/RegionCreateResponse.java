package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionCreateResult;

import java.util.UUID;

// 지역 등록 이후 응답값
public record RegionCreateResponse(
        UUID regionId
) {

    public static RegionCreateResponse from(
            RegionCreateResult result
    ) {
        return new RegionCreateResponse(
                result.regionId()
        );
    }
}