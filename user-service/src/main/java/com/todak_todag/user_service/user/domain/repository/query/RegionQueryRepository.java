package com.todak_todag.user_service.user.domain.repository.query;

import com.todak_todag.user_service.user.domain.entity.Region;

import java.util.List;

public interface RegionQueryRepository {

    // 서비스 가능한 지역 목록 조회
    List<Region> findAllAvailableRegions();
}