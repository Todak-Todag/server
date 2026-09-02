package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;

import java.util.List;

// 서비스 가능 지역 목록 조회 응답
public record RegionFindAvailableListResponse(
        List<RegionFindAvailableResponse> content
) {

    public static RegionFindAvailableListResponse of(List<RegionFindAvailableResult> results) {
        List<RegionFindAvailableResponse> content = results.stream()
                .map(RegionFindAvailableResponse::from)
                .toList();

        return new RegionFindAvailableListResponse(content);
    }
}