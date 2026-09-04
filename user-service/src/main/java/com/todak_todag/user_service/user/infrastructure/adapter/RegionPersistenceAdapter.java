package com.todak_todag.user_service.user.infrastructure.adapter;

import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.command.RegionCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RegionPersistenceAdapter implements RegionQueryRepository, RegionCommandRepository {

    private final JpaRegionRepository jpaRepository;

    @Override
    public List<Region> findAllAvailableRegions() {
        // 서비스 가능 지역 조회
        return jpaRepository.findAllByActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public Page<Region> findAllByAdminConditions(
            RegionFindAdminQuery query,
            Pageable pageable
    ) {
        // 관리자 지역 조건 검색
        return jpaRepository.findAllByAdminConditions(query, pageable);
    }

    @Override
    public boolean existsAvailableRegion(UUID regionId) {
        // 회원가입 시 지역 검증 조회
        return jpaRepository
                .existsByIdAndActiveTrueAndDeletedAtIsNull(regionId);
    }

    @Override
    public Optional<Region> findById(UUID regionId) {
        return jpaRepository.findByIdAndDeletedAtIsNull(regionId);
    }

    @Override
    public Region save(Region region) {
        return jpaRepository.save(region);
    }

    @Override
    public boolean existsByRegionCode(String regionCode) {
        return jpaRepository.existsByRegionCodeAndDeletedAtIsNull(regionCode);
    }
}