package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaRegionRepository
        extends JpaRepository<Region, UUID>, RegionQueryDslRepository {

    // 서비스 가능 + 미삭제 지역 조회
    List<Region> findAllByActiveTrueAndDeletedAtIsNull();

    // 로그인 시 지역 검증 : 서비스 기능 + 미삭제 지역 조회
    boolean existsByIdAndActiveTrueAndDeletedAtIsNull(UUID regionId);
}