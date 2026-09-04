package com.todak_todag.user_service.user.domain.repository.query;

import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.domain.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RegionQueryRepository {

    // 서비스 가능한 지역 목록 조회
    List<Region> findAllAvailableRegions();

    // 관리자 지역 목록 조회
    Page<Region> findAllByAdminConditions(
            RegionFindAdminQuery query,
            Pageable pageable
    );

    // 회원가입시 지역 검증을 위한 조회
    boolean existsAvailableRegion(UUID regionId);
}