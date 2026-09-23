package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.infrastructure.persistence.query.RegionQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRegionRepository
        extends JpaRepository<Region, UUID>, RegionQueryDslRepository {

    // 서비스 가능 + 미삭제 지역 조회
    List<Region> findAllByActiveTrueAndDeletedAtIsNull();

    // 회원가입 시 지역 검증 : 서비스 기능 + 미삭제 지역 조회
    boolean existsByIdAndActiveTrueAndDeletedAtIsNull(UUID regionId);

    // 지역 단건 조회
    Optional<Region> findByIdAndDeletedAtIsNull(UUID regionId);

    // 삭제되지 않은 지역 중 RegionCode 중복 조회 : 삭제된 코드 사용 가능
    boolean existsByRegionCodeAndDeletedAtIsNull(String regionCode);
}