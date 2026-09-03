package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.domain.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegionQueryDslRepository {

    // 관리자 지역 조건 검색
    Page<Region> findAllByAdminConditions(
            RegionFindAdminQuery query,
            Pageable pageable
    );
}