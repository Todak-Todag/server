package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.global.common.PageableFactory;
import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.application.result.RegionFindAdminResult;
import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionQueryService {

    private final RegionQueryRepository regionQueryRepository;

    public List<RegionFindAvailableResult> findAvailableRegions() {
        // 서비스 가능 지역 조회
        return regionQueryRepository.findAllAvailableRegions()
                .stream()
                .map(RegionFindAvailableResult::from)
                .toList();
    }

    public Page<RegionFindAdminResult> findAdminRegions(
            RegionFindAdminQuery query
    ) {
        // 공통 페이징 조건 생성
        Pageable pageable = PageableFactory.of(
                query.page(),
                query.size(),
                null
        );

        // 관리자 지역 조건 검색
        return regionQueryRepository
                .findAllByAdminConditions(query, pageable)
                .map(RegionFindAdminResult::from);
    }

    // 회원가입시 지역 검증 조회
    public boolean existsAvailableRegion(UUID regionId) {
        return regionQueryRepository.existsAvailableRegion(regionId);
    }
}